package com.benhsoan.infrastructure.authSecurity;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.exception.ApiErrorResponseFactory;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenPort jwtTokenPort;
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final ClockPort clockPort;
    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final ObjectMapper objectMapper;

    private volatile int cachedTimeoutMinutes = ClinicConfiguration.DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES;
    private volatile Instant cacheExpiresAt = Instant.MIN;

    @Autowired
    public JwtAuthenticationFilter(
            JwtTokenPort jwtTokenPort,
            UserSessionRepository userSessionRepository,
            UserRepository userRepository,
            ClockPort clockPort,
            @Autowired(required = false)
            ClinicConfigurationRepository clinicConfigurationRepository,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenPort = jwtTokenPort;
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
        this.clockPort = clockPort;
        this.clinicConfigurationRepository = clinicConfigurationRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    public JwtAuthenticationFilter(
            JwtTokenPort jwtTokenPort,
            UserSessionRepository userSessionRepository,
            UserRepository userRepository,
            ClockPort clockPort,
            ObjectMapper objectMapper
    ) {
        this(jwtTokenPort, userSessionRepository, userRepository, clockPort, null, objectMapper);
    }

    public JwtAuthenticationFilter(
            JwtTokenPort jwtTokenPort,
            UserSessionRepository userSessionRepository,
            UserRepository userRepository,
            ClockPort clockPort
    ) {
        this(jwtTokenPort, userSessionRepository, userRepository, clockPort, null, new ObjectMapper().findAndRegisterModules());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {

            String token = extractToken(request);

            if (token != null && jwtTokenPort.validate(token)) {
                UUID userId = jwtTokenPort.getUserId(token);
                UUID sessionId = jwtTokenPort.getSessionId(token);
                Instant now = clockPort.now();

                Optional<UserSession> sessionOptional = userSessionRepository.findById(sessionId)
                        .filter(session -> session.getUserId().equals(userId));

                if (sessionOptional.isPresent()) {
                    UserSession session = sessionOptional.get();

                    if (session.isRevoked()) {
                        SecurityContextHolder.clearContext();
                        if (!isPublicOrAuthPath(request)) {
                            writeUnauthorizedError(response, request, "SESSION_TERMINATED", "Phiên làm việc đã bị kết thúc bởi quản trị viên.");
                            return;
                        }
                    } else if (session.isRefreshExpired(now)) {
                        SecurityContextHolder.clearContext();
                        if (!isPublicOrAuthPath(request)) {
                            writeUnauthorizedError(response, request, "SESSION_EXPIRED", "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.");
                            return;
                        }
                    } else {
                        Duration idleTimeout = getIdleTimeout(now);
                        if (session.isIdleTimeout(now, idleTimeout)) {
                            SecurityContextHolder.clearContext();
                            if (!isPublicOrAuthPath(request)) {
                                writeUnauthorizedError(response, request, "SESSION_EXPIRED", "Phiên làm việc đã hết hạn do không thao tác. Vui lòng đăng nhập lại.");
                                return;
                            }
                        } else {
                            Optional<User> userOptional = userRepository.findById(userId);
                            boolean userIsActive = userOptional
                                    .map(User::isActive)
                                    .orElse(false);

                            if (userIsActive) {
                                User user = userOptional.get();

                                if (user.isMustChangePassword() && !isAllowedPathWhenMustChangePassword(request)) {
                                    SecurityContextHolder.clearContext();
                                    writeMustChangePasswordError(response, request);
                                    return;
                                }

                                // Throttled lastUsedAt update (>= 60 seconds)
                                Instant lastUsed = session.getLastUsedAt() != null ? session.getLastUsedAt() : session.getCreatedAt();
                                if (lastUsed == null || Duration.between(lastUsed, now).getSeconds() >= 60) {
                                    userSessionRepository.touchLastUsed(sessionId, now);
                                }

                                String username = jwtTokenPort.getUsername(token);
                                String role = jwtTokenPort.getRole(token);
                                CurrentUserPrincipal principal = new CurrentUserPrincipal(userId, username, sessionId);

                                List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                                jwtTokenPort.getPermissions(token).forEach(permission ->
                                        authorities.add(new SimpleGrantedAuthority("PERMISSION_" + permission)));
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                principal,
                                                null,
                                                authorities
                                        );

                                authentication.setDetails(
                                        new WebAuthenticationDetailsSource().buildDetails(request)
                                );

                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            } else {
                                SecurityContextHolder.clearContext();
                            }
                        }
                    }
                } else {
                    SecurityContextHolder.clearContext();
                }
            } else if (token != null) {
                SecurityContextHolder.clearContext();
            }

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    private Duration getIdleTimeout(Instant now) {
        if (clinicConfigurationRepository != null && now.isAfter(cacheExpiresAt)) {
            try {
                cachedTimeoutMinutes = clinicConfigurationRepository.find()
                        .map(ClinicConfiguration::getSessionIdleTimeoutMinutes)
                        .orElse(ClinicConfiguration.DEFAULT_SESSION_IDLE_TIMEOUT_MINUTES);
                cacheExpiresAt = now.plus(Duration.ofMinutes(5));
            } catch (Exception ignored) {
                // Keep cached value on exception
            }
        }
        return Duration.ofMinutes(cachedTimeoutMinutes);
    }

    private boolean isPublicOrAuthPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (!StringUtils.hasText(path)) {
            path = request.getRequestURI();
        }
        if (path == null) {
            return false;
        }
        String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return normalized.equals("/auth/login")
                || normalized.equals("/auth/patient/login")
                || normalized.equals("/auth/patient/register")
                || normalized.equals("/auth/2fa/verify")
                || normalized.equals("/auth/2fa/resend")
                || normalized.equals("/auth/logout")
                || normalized.equals("/auth/refresh")
                || normalized.startsWith("/portal")
                || normalized.startsWith("/swagger")
                || normalized.startsWith("/v3/api-docs")
                || normalized.startsWith("/actuator");
    }

    private boolean isAllowedPathWhenMustChangePassword(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        if (!StringUtils.hasText(path)) {
            path = request.getRequestURI();
        }
        if (path == null) {
            return false;
        }
        String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return normalized.endsWith("/auth/change-password")
                || normalized.endsWith("/auth/logout")
                || normalized.endsWith("/auth/refresh");
    }

    private void writeUnauthorizedError(
            HttpServletResponse response,
            HttpServletRequest request,
            String code,
            String message
    ) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiErrorResponseFactory.create(
                        HttpStatus.UNAUTHORIZED,
                        code,
                        message,
                        request.getRequestURI()
                ));
    }

    private void writeMustChangePasswordError(
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiErrorResponseFactory.create(
                        HttpStatus.FORBIDDEN,
                        "MUST_CHANGE_PASSWORD",
                        "Tài khoản đang yêu cầu đổi mật khẩu trước khi tiếp tục thao tác",
                        request.getRequestURI()
                ));
    }

    private String extractToken(
            HttpServletRequest request
    ) {

        String authorization =
                request.getHeader("Authorization");

        if (!StringUtils.hasText(authorization)) {
            return null;
        }

        if (!authorization.startsWith("Bearer ")) {
            return null;
        }

        String token =
                authorization.substring(7);

        return StringUtils.hasText(token)
                ? token
                : null;
    }
}

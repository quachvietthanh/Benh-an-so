package com.benhsoan.infrastructure.authSecurity;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.benhsoan.exception.ApiErrorResponseFactory;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Duration REFRESH_SESSION_TIMEOUT = Duration.ofDays(7);

    private final JwtTokenPort jwtTokenPort;
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public JwtAuthenticationFilter(
            JwtTokenPort jwtTokenPort,
            UserSessionRepository userSessionRepository,
            UserRepository userRepository,
            ClockPort clockPort,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenPort = jwtTokenPort;
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
        this.clockPort = clockPort;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    public JwtAuthenticationFilter(
            JwtTokenPort jwtTokenPort,
            UserSessionRepository userSessionRepository,
            UserRepository userRepository,
            ClockPort clockPort
    ) {
        this(jwtTokenPort, userSessionRepository, userRepository, clockPort, new ObjectMapper().findAndRegisterModules());
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
                Instant now = clockPort.now();

                boolean sessionIsActive = userSessionRepository.findById(jwtTokenPort.getSessionId(token))
                        .filter(session -> session.getUserId().equals(userId))
                        .filter(session -> session.isActive(now, REFRESH_SESSION_TIMEOUT))
                        .isPresent();

                Optional<User> userOptional = userRepository.findById(userId);
                boolean userIsActive = userOptional
                        .map(User::isActive)
                        .orElse(false);

                if (sessionIsActive && userIsActive) {
                    User user = userOptional.get();

                    if (user.isMustChangePassword() && !isAllowedPathWhenMustChangePassword(request)) {
                        SecurityContextHolder.clearContext();
                        writeMustChangePasswordError(response, request);
                        return;
                    }

                    String username = jwtTokenPort.getUsername(token);
                    String role = jwtTokenPort.getRole(token);
                    CurrentUserPrincipal principal = new CurrentUserPrincipal(userId, username);

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

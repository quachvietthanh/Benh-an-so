package com.benhsoan.infrastructure.authSecurity;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Duration REFRESH_SESSION_TIMEOUT = Duration.ofDays(7);

    private final JwtTokenPort jwtTokenPort;
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final ClockPort clockPort;

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

                boolean userIsActive = userRepository.findById(userId)
                        .map(user -> user.isActive())
                        .orElse(false);

                if (sessionIsActive && userIsActive) {
                    String username = jwtTokenPort.getUsername(token);
                    String role = jwtTokenPort.getRole(token);
                    CurrentUserPrincipal principal = new CurrentUserPrincipal(userId, username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
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

package com.benhsoan.application.ucservice.auth;

import java.time.Instant;
import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.AccountDisabledException;
import com.benhsoan.domain.auth.exception.InvalidPasswordException;
import com.benhsoan.domain.auth.exception.TooManyLoginAttemptsException;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.port.dto.command.auth.LoginCommand;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.inbound.auth.LoginUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.authSecurity.RefreshTokenGeneratorPort;
import com.benhsoan.port.outbound.authSecurity.TokenHashPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService implements LoginUseCase {

    private static final Duration REFRESH_TOKEN_TIMEOUT = Duration.ofDays(7);

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserSessionRepository userSessionRepository;

    private final PasswordEncoderPort passwordEncoderPort;

    private final JwtTokenPort jwtTokenPort;

    private final TokenHashPort tokenHashPort;

    private final RefreshTokenGeneratorPort refreshTokenGeneratorPort;

    private final LoginAttemptPort loginAttemptPort;

    private final AuditLogRepository auditLogRepository;

    private final ClockPort clockPort;

    @Override
    public LoginResult login(LoginCommand command) {

        String username = command.username();

        if (loginAttemptPort.isBlocked(username)) {
            throw new TooManyLoginAttemptsException();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {

                    loginAttemptPort.loginFailed(username);

                    return new UserNotFoundException();
                });

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        if (!passwordEncoderPort.matches(
                command.password(),
                user.getPasswordHash()
        )) {

            loginAttemptPort.loginFailed(username);

            throw new InvalidPasswordException();
        }

        loginAttemptPort.loginSucceeded(username);

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(IllegalStateException::new);

        Instant now = clockPort.now();

        userSessionRepository.revokeByUserId(user.getId(), now);

        String refreshToken = refreshTokenGeneratorPort.generate();

        UserSession session =
                UserSession.create(
                        user.getId(),
                        tokenHashPort.hash(refreshToken),
                        now.plus(REFRESH_TOKEN_TIMEOUT)
                );

        userSessionRepository.save(session);

        String accessToken = jwtTokenPort.generateToken(
                user.getId(), session.getId(), user.getUsername(), role.getName(),
                role.getPermissions().stream().map(permission -> permission.getCode()).collect(java.util.stream.Collectors.toSet()));
        Instant expiredAt = jwtTokenPort.getExpiredAt(accessToken);

        user.updateLastLogin(now);

        userRepository.save(user);

        auditLogRepository.save(
                AuditLog.create(
                        user.getId(),
                        ActionType.LOGIN,
                        ResourceType.USER_SESSION,
                        session.getId(),
                        """
                        {
                        "username":"%s"
                        }
                        """.formatted(user.getUsername()),
                        null
                )
        );


        return new LoginResult(
                user.getId(),
                user.getUsername(),
                accessToken,
                refreshToken,
                role.getName(),
                expiredAt
        );
    }
}

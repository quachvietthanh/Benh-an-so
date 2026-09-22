package com.benhsoan.application.ucservice.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.Role;
import com.benhsoan.domain.auth.TwoFactorChallenge;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.domain.auth.exception.AccountDisabledException;
import com.benhsoan.domain.auth.exception.InvalidVerificationCodeException;
import com.benhsoan.domain.auth.exception.TwoFactorChallengeInvalidException;
import com.benhsoan.domain.auth.exception.VerificationCodeExpiredException;
import com.benhsoan.port.dto.command.auth.ResendTwoFactorCommand;
import com.benhsoan.port.dto.command.auth.VerifyTwoFactorCommand;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.dto.result.TwoFactorResendResult;
import com.benhsoan.port.inbound.auth.TwoFactorResendUseCase;
import com.benhsoan.port.inbound.auth.TwoFactorVerificationUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.authSecurity.RefreshTokenGeneratorPort;
import com.benhsoan.port.outbound.authSecurity.TokenHashPort;
import com.benhsoan.port.outbound.authSecurity.VerificationCodeGeneratorPort;
import com.benhsoan.port.outbound.notification.TwoFactorCodeDeliveryPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.TwoFactorChallengeRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TwoFactorAuthenticationService implements TwoFactorVerificationUseCase, TwoFactorResendUseCase {

    public static final long CODE_TTL_SECONDS = 300; // 5 minutes

    private static final Duration REFRESH_TOKEN_TIMEOUT = Duration.ofDays(7);

    private final TwoFactorChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoderPort passwordEncoderPort;
    private final JwtTokenPort jwtTokenPort;
    private final TokenHashPort tokenHashPort;
    private final RefreshTokenGeneratorPort refreshTokenGeneratorPort;
    private final VerificationCodeGeneratorPort codeGeneratorPort;
    private final TwoFactorCodeDeliveryPort codeDeliveryPort;
    private final AuditLogRepository auditLogRepository;
    private final TwoFactorSecurityAuditWriter auditWriter;
    private final ClockPort clockPort;

    /**
     * Creates a temporary two-factor challenge after password authentication succeeded
     * and delivers the simulated verification code. No normal session or JWT is created.
     */
    public TwoFactorChallenge issueChallenge(User user, Instant now) {
        String plainCode = codeGeneratorPort.generate();
        String codeHash = passwordEncoderPort.encode(plainCode);
        Instant expiresAt = now.plusSeconds(CODE_TTL_SECONDS);

        TwoFactorChallenge challenge = TwoFactorChallenge.create(user.getId(), codeHash, expiresAt, now);
        challengeRepository.save(challenge);
        codeDeliveryPort.sendVerificationCode(user.getUsername(), plainCode, CODE_TTL_SECONDS);

        return challenge;
    }

    @Override
    public LoginResult verify(VerifyTwoFactorCommand command) {
        Instant now = clockPort.now();
        TwoFactorChallenge challenge = resolveChallenge(command.twoFactorToken());

        if (challenge.isConsumed()) {
            auditWriter.recordFailedVerification(challenge.getId(), challenge.getUserId(), "CHALLENGE_ALREADY_CONSUMED", null, now);
            throw new TwoFactorChallengeInvalidException();
        }

        if (challenge.isExpired(now)) {
            auditWriter.recordFailedVerification(challenge.getId(), challenge.getUserId(), "EXPIRED_CODE", null, now);
            throw new VerificationCodeExpiredException();
        }

        if (!passwordEncoderPort.matches(command.code(), challenge.getCodeHash())) {
            auditWriter.recordFailedVerification(challenge.getId(), challenge.getUserId(), "INVALID_CODE", null, now);
            throw new InvalidVerificationCodeException();
        }

        // Atomically consume the challenge; a concurrent verification returning 0 means
        // another request already consumed it, so this request must not authenticate again.
        int consumed = challengeRepository.markConsumed(challenge.getId(), now);
        if (consumed == 0) {
            throw new TwoFactorChallengeInvalidException();
        }
        challenge.markConsumed(now);

        User user = userRepository.findById(challenge.getUserId())
                .orElseThrow(TwoFactorChallengeInvalidException::new);

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(IllegalStateException::new);

        return completeLogin(user, role, now);
    }

    @Override
    public TwoFactorResendResult resend(ResendTwoFactorCommand command) {
        Instant now = clockPort.now();
        TwoFactorChallenge challenge = resolveChallenge(command.twoFactorToken());

        if (challenge.isConsumed()) {
            throw new TwoFactorChallengeInvalidException();
        }

        String plainCode = codeGeneratorPort.generate();
        String codeHash = passwordEncoderPort.encode(plainCode);
        Instant expiresAt = now.plusSeconds(CODE_TTL_SECONDS);

        challenge.rotateCode(codeHash, expiresAt);
        challengeRepository.save(challenge);

        User user = userRepository.findById(challenge.getUserId()).orElse(null);
        String username = user != null ? user.getUsername() : "unknown";
        codeDeliveryPort.sendVerificationCode(username, plainCode, CODE_TTL_SECONDS);

        return new TwoFactorResendResult(challenge.getId(), expiresAt);
    }

    private LoginResult completeLogin(User user, Role role, Instant now) {
        userSessionRepository.revokeByUserId(user.getId(), now);

        String refreshToken = refreshTokenGeneratorPort.generate();
        UserSession session = UserSession.create(
                user.getId(),
                tokenHashPort.hash(refreshToken),
                now.plus(REFRESH_TOKEN_TIMEOUT)
        );
        userSessionRepository.save(session);

        String accessToken = jwtTokenPort.generateToken(
                user.getId(), session.getId(), user.getUsername(), role.getName(),
                role.getPermissions().stream().map(permission -> permission.getCode())
                        .collect(java.util.stream.Collectors.toSet())
        );
        Instant expiredAt = jwtTokenPort.getExpiredAt(accessToken);

        user.updateLastLogin(now);
        userRepository.save(user);

        auditLogRepository.save(AuditLog.create(
                user.getId(),
                ActionType.LOGIN,
                ResourceType.USER_SESSION,
                session.getId(),
                """
                {
                  "username": "%s",
                  "twoFactor": true
                }
                """.formatted(user.getUsername()),
                null,
                now
        ));

        return new LoginResult(
                user.getId(),
                user.getUsername(),
                accessToken,
                refreshToken,
                role.getName(),
                expiredAt,
                user.isMustChangePassword()
        );
    }

    private TwoFactorChallenge resolveChallenge(String token) {
        UUID id;
        try {
            id = UUID.fromString(token);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new TwoFactorChallengeInvalidException();
        }
        return challengeRepository.findById(id)
                .orElseThrow(TwoFactorChallengeInvalidException::new);
    }
}

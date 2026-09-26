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
import com.benhsoan.domain.auth.exception.VerificationCodeCooldownException;
import com.benhsoan.domain.auth.exception.VerificationCodeExpiredException;
import com.benhsoan.port.dto.command.auth.ResendTwoFactorCommand;
import com.benhsoan.port.dto.command.auth.VerifyTwoFactorCommand;
import com.benhsoan.port.dto.result.LoginResult;
import com.benhsoan.port.dto.result.TwoFactorResendResult;
import com.benhsoan.port.inbound.auth.TwoFactorResendUseCase;
import com.benhsoan.port.inbound.auth.TwoFactorVerificationUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.authSecurity.PasswordEncoderPort;
import com.benhsoan.port.outbound.authSecurity.PatientRecoveryCooldownPort;
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
    private static final long RESEND_COOLDOWN_SECONDS = 60;

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
    private final PatientRecoveryCooldownPort cooldownPort;
    private final ClockPort clockPort;

    /**
     * Creates a temporary two-factor challenge after password authentication succeeded
     * and delivers the simulated verification code. No normal session or JWT is created.
     *
     * <p>Any previously pending challenge for the same user is invalidated first so only
     * the most recently issued challenge remains usable (single-active-challenge policy).</p>
     */
    public TwoFactorChallenge issueChallenge(User user, Instant now) {
        challengeRepository.invalidatePendingChallengesByUserId(user.getId(), now);

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
        String ipAddress = command.ipAddress();

        UUID challengeId = parseChallengeId(command.twoFactorToken());
        if (challengeId == null) {
            auditWriter.recordFailure(null, null, "INVALID_CHALLENGE_TOKEN", ipAddress, now);
            throw new TwoFactorChallengeInvalidException();
        }

        TwoFactorChallenge challenge = challengeRepository.findById(challengeId).orElse(null);
        if (challenge == null) {
            auditWriter.recordFailure(challengeId, null, "CHALLENGE_NOT_FOUND", ipAddress, now);
            throw new TwoFactorChallengeInvalidException();
        }

        UUID userId = challenge.getUserId();

        if (challenge.isConsumed()) {
            auditWriter.recordFailure(challengeId, userId, "CHALLENGE_CONSUMED", ipAddress, now);
            throw new TwoFactorChallengeInvalidException();
        }

        if (challenge.isExpired(now)) {
            auditWriter.recordFailure(challengeId, userId, "CHALLENGE_EXPIRED", ipAddress, now);
            throw new VerificationCodeExpiredException();
        }

        // Brute-force guard: reject without performing a BCrypt comparison once the
        // challenge has reached the maximum number of failed attempts.
        if (challenge.isAttemptsExceeded()) {
            auditWriter.recordFailure(challengeId, userId, "MAX_ATTEMPTS_EXCEEDED", ipAddress, now);
            throw new InvalidVerificationCodeException(
                    "Mã xác thực đã bị vô hiệu hóa do nhập sai quá số lần cho phép. Vui lòng yêu cầu mã mới.");
        }

        if (!passwordEncoderPort.matches(command.code(), challenge.getCodeHash())) {
            auditWriter.recordInvalidCode(challengeId, userId, ipAddress, now);
            throw new InvalidVerificationCodeException();
        }

        // Atomically consume the challenge; a concurrent verification returning 0 means
        // another request already consumed it, so this request must not authenticate again.
        int consumed = challengeRepository.markConsumed(challengeId, now);
        if (consumed == 0) {
            auditWriter.recordFailure(challengeId, userId, "CONCURRENT_CONSUMPTION_CONFLICT", ipAddress, now);
            throw new TwoFactorChallengeInvalidException();
        }

        // Re-validate account state at the final authentication step: password success
        // does not guarantee the account is still active when 2FA completes.
        User user = userRepository.findById(userId)
                .orElseThrow(TwoFactorChallengeInvalidException::new);

        if (!user.isActive()) {
            auditWriter.recordFailure(challengeId, userId, "ACCOUNT_DISABLED", ipAddress, now);
            throw new AccountDisabledException();
        }

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(IllegalStateException::new);

        return completeLogin(user, role, now, ipAddress, command.userAgent());
    }

    @Override
    public TwoFactorResendResult resend(ResendTwoFactorCommand command) {
        Instant now = clockPort.now();
        TwoFactorChallenge challenge = resolveChallengeForResend(command.twoFactorToken());

        if (challenge.isConsumed()) {
            throw new TwoFactorChallengeInvalidException();
        }

        if (challenge.isLifetimeExceeded(now)) {
            throw new TwoFactorChallengeInvalidException(
                    "Thử thách xác thực hai lớp đã quá thời hạn tối đa. Vui lòng đăng nhập lại.");
        }

        String cooldownKey = challenge.getId().toString();
        if (cooldownPort.isInCooldown(cooldownKey, now)) {
            long remaining = cooldownPort.getRemainingCooldownSeconds(cooldownKey, now);
            throw new VerificationCodeCooldownException(remaining);
        }

        String plainCode = codeGeneratorPort.generate();
        String codeHash = passwordEncoderPort.encode(plainCode);
        Instant cappedExpiry = capExpiryWithinLifetime(challenge, now);

        challenge.rotateCode(codeHash, cappedExpiry);
        challengeRepository.save(challenge);
        cooldownPort.recordRequest(cooldownKey, now, RESEND_COOLDOWN_SECONDS);

        User user = userRepository.findById(challenge.getUserId()).orElse(null);
        String username = user != null ? user.getUsername() : "unknown";
        codeDeliveryPort.sendVerificationCode(username, plainCode, CODE_TTL_SECONDS);

        return new TwoFactorResendResult(challenge.getId(), cappedExpiry);
    }

    /**
     * Caps the new code expiry so the challenge can never be kept alive beyond its
     * maximum lifetime measured from the original {@code createdAt}.
     */
    private Instant capExpiryWithinLifetime(TwoFactorChallenge challenge, Instant now) {
        Instant codeExpiry = now.plusSeconds(CODE_TTL_SECONDS);
        Instant maxLifetime = challenge.getCreatedAt().plusSeconds(TwoFactorChallenge.MAX_LIFETIME_SECONDS);
        return codeExpiry.isAfter(maxLifetime) ? maxLifetime : codeExpiry;
    }

    private LoginResult completeLogin(User user, Role role, Instant now, String ipAddress, String userAgent) {
        userSessionRepository.revokeByUserId(user.getId(), now);

        String refreshToken = refreshTokenGeneratorPort.generate();
        UserSession session = UserSession.create(
                user.getId(),
                tokenHashPort.hash(refreshToken),
                now.plus(REFRESH_TOKEN_TIMEOUT),
                ipAddress,
                userAgent
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
                ipAddress,
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

    private UUID parseChallengeId(String token) {
        try {
            return UUID.fromString(token);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

    private TwoFactorChallenge resolveChallengeForResend(String token) {
        UUID id = parseChallengeId(token);
        if (id == null) {
            throw new TwoFactorChallengeInvalidException();
        }
        return challengeRepository.findById(id)
                .orElseThrow(TwoFactorChallengeInvalidException::new);
    }
}

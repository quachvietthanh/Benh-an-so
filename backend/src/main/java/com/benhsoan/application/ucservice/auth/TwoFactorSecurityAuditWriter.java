package com.benhsoan.application.ucservice.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.TwoFactorChallenge;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.TwoFactorChallengeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Persists two-factor verification failure audit records in an independent
 * transaction (REQUIRES_NEW) so they survive the business transaction rollback
 * triggered by the subsequent authentication exception.
 *
 * <p>Only an invalid OTP increments the attempt counter; all other failure reasons
 * are audit-only so the brute-force counter is not polluted by expired/consumed/
 * malformed-challenge events.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwoFactorSecurityAuditWriter {

    private final TwoFactorChallengeRepository challengeRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Records an invalid-OTP attempt: atomically increments the bounded attempt counter
     * and writes the audit entry. Used exclusively for {@code INVALID_CODE}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInvalidCode(UUID challengeId, UUID userId, String ipAddress, Instant now) {
        challengeRepository.incrementAttempts(challengeId, TwoFactorChallenge.MAX_ATTEMPTS);
        write(userId, "INVALID_CODE", challengeId, ipAddress, now);
    }

    /**
     * Records a failure that must not increment the attempt counter (expired, consumed,
     * max attempts exceeded, not found, malformed, concurrent conflict, disabled account).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID challengeId, UUID userId, String reason, String ipAddress, Instant now) {
        write(userId, reason, challengeId, ipAddress, now);
    }

    private void write(UUID userId, String reason, UUID challengeId, String ipAddress, Instant now) {
        String detail = """
                {
                  "userId": "%s",
                  "reason": "%s",
                  "twoFactor": true
                }
                """.formatted(userId != null ? userId.toString() : "", reason);

        try {
            auditLogRepository.save(AuditLog.create(
                    userId,
                    ActionType.LOGIN_FAILED,
                    ResourceType.USER_SESSION,
                    challengeId,
                    detail,
                    ipAddress,
                    now
            ));
        } catch (Exception ex) {
            log.error("Failed to persist security audit log for two-factor authentication failure", ex);
        }
    }
}

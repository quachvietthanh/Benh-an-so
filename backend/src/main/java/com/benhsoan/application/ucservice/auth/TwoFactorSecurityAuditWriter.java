package com.benhsoan.application.ucservice.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.TwoFactorChallengeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Persists failed two-factor verification attempt counters and audit records in an
 * independent transaction (REQUIRES_NEW) so they survive the business transaction
 * rollback triggered by the subsequent authentication exception.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwoFactorSecurityAuditWriter {

    private final TwoFactorChallengeRepository challengeRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedVerification(
            UUID challengeId,
            UUID userId,
            String reason,
            String ipAddress,
            Instant now
    ) {
        challengeRepository.incrementAttempts(challengeId);

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

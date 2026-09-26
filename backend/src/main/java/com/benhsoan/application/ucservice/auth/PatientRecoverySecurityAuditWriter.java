package com.benhsoan.application.ucservice.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.PatientPasswordRecoveryToken;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.PatientPasswordRecoveryTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Persists recovery token failed-attempt increments and security audit logs in an
 * independent transaction (REQUIRES_NEW) so they survive business transaction rollbacks (Finding 1 & 6).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientRecoverySecurityAuditWriter {

    private final PatientPasswordRecoveryTokenRepository tokenRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttemptAndAudit(
            PatientPasswordRecoveryToken token,
            UUID userId,
            String phone,
            String ipAddress,
            Instant now
    ) {
        tokenRepository.incrementAttempts(token.getId());
        int updatedAttempts = tokenRepository.getAttempts(token.getId());
        token.setAttempts(updatedAttempts);

        if (token.isAttemptsExceeded()) {
            UUID actorId = userId != null ? userId : token.getUserId();
            String detail = """
                    {
                      "phone": "%s",
                      "failedAttempts": %d,
                      "reason": "Token locked due to maximum failed attempts exceeded (Anti-Brute-Force)"
                    }
                    """.formatted(phone, updatedAttempts);

            try {
                auditLogRepository.save(AuditLog.create(
                        actorId,
                        ActionType.ACCESS_DENIED,
                        ResourceType.PATIENT_PORTAL,
                        token.getId(),
                        detail,
                        ipAddress,
                        now
                ));
            } catch (Exception ex) {
                log.error("Failed to persist security audit log for recovery code lockout", ex);
            }
        }
    }
}

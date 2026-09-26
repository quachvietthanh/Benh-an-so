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

import lombok.RequiredArgsConstructor;

/**
 * Writes account lockout audit entries in an independent transaction (REQUIRES_NEW)
 * so the lockout record survives the transaction rollback triggered by the subsequent
 * {@code TooManyLoginAttemptsException}.
 */
@Component
@RequiredArgsConstructor
public class LoginLockoutAuditWriter {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeUsernameLockout(
            UUID userId,
            String username,
            int failedAttempts,
            Instant blockedUntil,
            String ipAddress
    ) {
        String detail = """
                {
                "username":"%s",
                "failedAttempts":%d,
                "blockedUntil":"%s"
                }
                """.formatted(
                username,
                failedAttempts,
                blockedUntil != null ? blockedUntil.toString() : ""
        );

        auditLogRepository.save(
                AuditLog.create(
                        userId,
                        ActionType.LOCK,
                        ResourceType.USER,
                        userId,
                        detail,
                        ipAddress
                )
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writePhoneLockout(
            UUID userId,
            String phone,
            int failedAttempts,
            Instant blockedUntil,
            String ipAddress
    ) {
        String detail = """
                {
                "phone":"%s",
                "failedAttempts":%d,
                "blockedUntil":"%s"
                }
                """.formatted(
                phone,
                failedAttempts,
                blockedUntil != null ? blockedUntil.toString() : ""
        );

        auditLogRepository.save(
                AuditLog.create(
                        userId,
                        ActionType.LOCK,
                        ResourceType.USER,
                        userId,
                        detail,
                        ipAddress
                )
        );
    }
}

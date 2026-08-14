package com.benhsoan.application.ucservice.backup;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BackupAuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(UUID actorId, ActionType action, UUID resourceId, String detail) {
        auditLogRepository.save(AuditLog.create(
                actorId,
                action,
                ResourceType.SYSTEM_BACKUP,
                resourceId,
                detail,
                null
        ));
    }
}

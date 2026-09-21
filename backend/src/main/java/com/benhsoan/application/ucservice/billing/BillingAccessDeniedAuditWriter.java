package com.benhsoan.application.ucservice.billing;

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

@Component
@RequiredArgsConstructor
public class BillingAccessDeniedAuditWriter {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAccessDenied(
            UUID actorId,
            ResourceType resourceType,
            UUID resourceId,
            String detail,
            Instant timestamp
    ) {
        if (actorId == null) {
            return;
        }

        AuditLog log = AuditLog.create(
                actorId,
                ActionType.ACCESS_DENIED,
                resourceType,
                resourceId,
                detail,
                null,
                timestamp != null ? timestamp : Instant.now()
        );
        auditLogRepository.save(log);
    }
}

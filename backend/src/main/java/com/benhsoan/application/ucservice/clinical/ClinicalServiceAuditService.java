package com.benhsoan.application.ucservice.clinical;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicalServiceAuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(UUID actorId, ActionType action, UUID serviceId, String detail, Instant at) {
        String safeDetail = (detail != null && detail.trim().startsWith("{"))
                ? detail
                : "{\"before\":null,\"after\":null,\"summary\":\"" + (detail != null ? detail.replace("\"", "\\\"") : "") + "\"}";
        auditLogRepository.save(AuditLog.create(
                actorId, action, ResourceType.CLINICAL_SERVICE, serviceId, safeDetail, null, at
        ));
    }
}

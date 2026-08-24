package com.benhsoan.application.ucservice.medicalrecord;

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
 * Writes medical record amendment audit entries (NCL-11-CN-002). The denial entry
 * is written in an independent transaction so it survives the rollback triggered by
 * the authorization failure (TC-03).
 */
@Component
@RequiredArgsConstructor
public class MedicalRecordAmendmentAuditWriter {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeDenied(UUID actorId, UUID medicalRecordId, String detail, Instant now) {
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.ACCESS_DENIED,
                ResourceType.MEDICAL_RECORD,
                medicalRecordId,
                detail,
                null,
                now
        ));
    }

    @Transactional
    public void writeAmended(UUID actorId, UUID medicalRecordId, String reason, Instant amendedAt) {
        String detail = "Medical record amendment: amendedBy=" + actorId
                + ", reason=" + reason + ", amendedAt=" + amendedAt;
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.UPDATE,
                ResourceType.MEDICAL_RECORD,
                medicalRecordId,
                detail,
                null,
                amendedAt
        ));
    }
}

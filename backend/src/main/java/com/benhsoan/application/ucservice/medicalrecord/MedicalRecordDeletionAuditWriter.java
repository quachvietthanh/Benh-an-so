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
 * Writes medical-record deletion audit entries. The denial entry is written in
 * an independent transaction so it survives the rollback triggered by the
 * {@code MedicalRecordRetentionException} (QTN-19).
 */
@Component
@RequiredArgsConstructor
public class MedicalRecordDeletionAuditWriter {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeDenied(UUID actorId, UUID medicalRecordId, Instant now) {
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.ACCESS_DENIED,
                ResourceType.MEDICAL_RECORD,
                medicalRecordId,
                "Medical record deletion denied: within minimum retention period.",
                null,
                now
        ));
    }

    @Transactional
    public void writeDeleted(UUID actorId, UUID medicalRecordId, Instant now) {
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.DELETE,
                ResourceType.MEDICAL_RECORD,
                medicalRecordId,
                "Medical record deleted.",
                null,
                now
        ));
    }
}

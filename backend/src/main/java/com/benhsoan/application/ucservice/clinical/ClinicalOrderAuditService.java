package com.benhsoan.application.ucservice.clinical;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicalOrderAuditService {

    private final MedicalRecordAccessLogRepository medicalRecordAccessLogRepository;
    private final AuditLogRepository auditLogRepository;

    public void recordCreated(UUID patientId, UUID visitId, UUID medicalRecordId, UUID actorId, Instant createdAt) {
        medicalRecordAccessLogRepository.save(MedicalRecordAccessLog.createRecordAccess(
                patientId, visitId, medicalRecordId, actorId, MedicalRecordAccessAction.CREATE,
                "Clinical order created", createdAt
        ));
    }

    public void recordCancelled(UUID patientId, UUID visitId, UUID medicalRecordId, UUID actorId, String reason, Instant cancelledAt) {
        medicalRecordAccessLogRepository.save(MedicalRecordAccessLog.createRecordAccess(
                patientId, visitId, medicalRecordId, actorId, MedicalRecordAccessAction.UPDATE,
                "Clinical order cancelled: " + reason, cancelledAt
        ));
    }

    public void recordViewPending(UUID actorId, Instant viewedAt) {
        auditLogRepository.save(AuditLog.create(
                actorId, ActionType.READ, ResourceType.CLINICAL_SERVICE, null,
                "Pending clinical orders viewed", null, viewedAt
        ));
    }
}

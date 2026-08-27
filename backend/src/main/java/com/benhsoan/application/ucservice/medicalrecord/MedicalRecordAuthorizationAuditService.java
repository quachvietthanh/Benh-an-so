package com.benhsoan.application.ucservice.medicalrecord;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalRecordAuthorizationAuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordDiagnosisWriteDenied(UUID actorId, UUID medicalRecordId) {
        recordTemplateAccessDenied(actorId, medicalRecordId, "Diagnosis write access denied");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordContentWriteDenied(UUID actorId, UUID medicalRecordId) {
        recordTemplateAccessDenied(actorId, medicalRecordId, "Medical record write access denied");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTemplateAccessDenied(UUID actorId, UUID medicalRecordId, String detail) {
        try {
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.ACCESS_DENIED,
                    ResourceType.MEDICAL_RECORD,
                    medicalRecordId,
                    detail,
                    null
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to record access denied audit log for actor {} on medical record {}: {}",
                    actorId, medicalRecordId, exception.getMessage());
        }
    }
}

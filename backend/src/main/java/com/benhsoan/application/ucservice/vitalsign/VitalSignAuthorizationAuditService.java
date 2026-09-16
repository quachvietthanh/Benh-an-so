package com.benhsoan.application.ucservice.vitalsign;

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
public class VitalSignAuthorizationAuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordVisitAccessDenied(UUID actorId, UUID visitId, String detail) {
        recordAccessDenied(actorId, ResourceType.VISIT, visitId, detail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPatientAccessDenied(UUID actorId, UUID patientId, String detail) {
        recordAccessDenied(actorId, ResourceType.PATIENT, patientId, detail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordVitalSignAccessDenied(UUID actorId, UUID vitalSignId, String detail) {
        recordAccessDenied(actorId, ResourceType.VITAL_SIGN, vitalSignId, detail);
    }

    private void recordAccessDenied(UUID actorId, ResourceType resourceType, UUID resourceId, String detail) {
        try {
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.ACCESS_DENIED,
                    resourceType,
                    resourceId,
                    detail,
                    null
            ));
        } catch (RuntimeException exception) {
            log.warn("Failed to record access denied audit log for actor {} on {} {}: {}",
                    actorId, resourceType, resourceId, exception.getMessage());
        }
    }
}

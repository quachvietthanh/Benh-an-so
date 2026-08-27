package com.benhsoan.application.ucservice.medicalrecord;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MedicalRecordAuthorizationService {

    private final CurrentUserPort currentUserPort;
    private final PermissionEvaluator permissionEvaluator;
    private final MedicalRecordAuthorizationAuditService authorizationAuditService;

    public UUID requireReadAccess() {
        if (!currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("DOCTOR")) {
            throw new MedicalRecordAccessDeniedException();
        }
        return currentUserPort.getCurrentUserId();
    }

    public UUID requireVersionHistoryReadAccess() {
        if (!currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("MANAGER")) {
            throw new MedicalRecordAccessDeniedException();
        }
        return currentUserPort.getCurrentUserId();
    }

    public UUID requireWriteAccess() {
        if (!currentUserPort.hasRole("ADMIN") && !currentUserPort.hasRole("DOCTOR")) {
            throw new MedicalRecordAccessDeniedException();
        }
        return currentUserPort.getCurrentUserId();
    }

    public UUID requireContentWriteAccess(UUID medicalRecordId) {
        UUID actorId = currentUserPort.getCurrentUserId();
        if (!currentUserPort.hasRole("DOCTOR") || currentUserPort.hasRole("ADMIN")) {
            authorizationAuditService.recordContentWriteDenied(actorId, medicalRecordId);
            throw new MedicalRecordAccessDeniedException();
        }
        return actorId;
    }

    public void requireContentVisitWriteAccess(UUID actorId, UUID visitDoctorId, UUID medicalRecordId) {
        if (!actorId.equals(visitDoctorId)) {
            authorizationAuditService.recordContentWriteDenied(actorId, medicalRecordId);
            throw new MedicalRecordAccessDeniedException();
        }
    }

    public UUID requireDiagnosisWriteAccess(UUID medicalRecordId) {
        UUID actorId = currentUserPort.getCurrentUserId();
        if (!currentUserPort.hasRole("DOCTOR") || currentUserPort.hasRole("ADMIN")) {
            authorizationAuditService.recordDiagnosisWriteDenied(actorId, medicalRecordId);
            throw new MedicalRecordAccessDeniedException();
        }
        return actorId;
    }

    public UUID requireTemplateReadAccess(UUID medicalRecordId) {
        return requireDoctorTemplateAccess(medicalRecordId);
    }

    public UUID requireTemplateWriteAccess(UUID medicalRecordId) {
        return requireDoctorTemplateAccess(medicalRecordId);
    }

    public void requireTemplateVisitAccess(UUID actorId, UUID visitDoctorId, UUID medicalRecordId) {
        if (!actorId.equals(visitDoctorId)) {
            authorizationAuditService.recordTemplateAccessDenied(actorId, medicalRecordId,
                    "Medical record template access denied");
            throw new MedicalRecordAccessDeniedException();
        }
    }

    public void requireDiagnosisVisitWriteAccess(UUID actorId, UUID visitDoctorId, UUID medicalRecordId) {
        if (!actorId.equals(visitDoctorId)) {
            authorizationAuditService.recordDiagnosisWriteDenied(actorId, medicalRecordId);
            throw new MedicalRecordAccessDeniedException();
        }
    }

    public UUID requireAmendAccess(UUID visitDoctorId) {
        UUID userId = currentUserPort.getCurrentUserId();
        if (!currentUserPort.hasRole("DOCTOR") || !visitDoctorId.equals(userId)) {
            throw new MedicalRecordAccessDeniedException();
        }
        return userId;
    }

    public UUID requireAuditReadAccess() {
        if (!permissionEvaluator.hasPermission("AUDIT_READ")) {
            throw new MedicalRecordAccessDeniedException();
        }
        return currentUserPort.getCurrentUserId();
    }

    private UUID requireDoctorTemplateAccess(UUID medicalRecordId) {
        UUID actorId = currentUserPort.getCurrentUserId();
        if (!currentUserPort.hasRole("DOCTOR") || currentUserPort.hasRole("ADMIN")) {
            authorizationAuditService.recordTemplateAccessDenied(actorId, medicalRecordId,
                    "Medical record template access denied");
            throw new MedicalRecordAccessDeniedException();
        }
        return actorId;
    }
}

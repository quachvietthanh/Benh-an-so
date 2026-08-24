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

    public UUID requireAuditReadAccess() {
        if (!permissionEvaluator.hasPermission("AUDIT_READ")) {
            throw new MedicalRecordAccessDeniedException();
        }
        return currentUserPort.getCurrentUserId();
    }
}

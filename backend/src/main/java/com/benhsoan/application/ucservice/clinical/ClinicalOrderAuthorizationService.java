package com.benhsoan.application.ucservice.clinical;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClinicalOrderAuthorizationService {

    private final CurrentUserPort currentUserPort;

    public UUID requireReadAccess() {
        if (!currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("DOCTOR")
                && !currentUserPort.hasRole("NURSE")) {
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
}

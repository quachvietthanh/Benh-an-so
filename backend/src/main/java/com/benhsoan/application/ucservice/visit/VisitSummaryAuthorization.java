package com.benhsoan.application.ucservice.visit;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VisitSummaryAuthorization {

    private final CurrentUserPort currentUserPort;

    public void requireSummaryAccess(UUID visitDoctorId) {
        if (currentUserPort.hasRole("ADMIN")
                || currentUserPort.hasRole("MANAGER")
                || currentUserPort.hasRole("RECEPTIONIST")) {
            return;
        }
        if (currentUserPort.hasRole("DOCTOR")
                && visitDoctorId != null
                && visitDoctorId.equals(currentUserPort.getCurrentUserId())) {
            return;
        }
        throw new MedicalRecordAccessDeniedException("Bác sĩ chỉ có quyền xem và in phiếu tóm tắt của lượt khám do mình phụ trách.");
    }
}

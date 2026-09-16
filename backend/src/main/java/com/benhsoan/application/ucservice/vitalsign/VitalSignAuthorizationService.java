package com.benhsoan.application.ucservice.vitalsign;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VitalSignAuthorizationService {

    private final CurrentUserPort currentUserPort;

    public UUID requireReadAccess() {
        return currentUserPort.getCurrentUserId();
    }

    public UUID requireWriteAccess() {
        return currentUserPort.getCurrentUserId();
    }

    public void requireVisitDoctorAccess(UUID visitDoctorId) {
        UUID currentUserId = currentUserPort.getCurrentUserId();
        if (!currentUserId.equals(visitDoctorId)) {
            throw new MedicalRecordAccessDeniedException("Chỉ bác sĩ phụ trách lượt khám mới có quyền ghi/sửa chỉ số sinh tồn.");
        }
    }
}

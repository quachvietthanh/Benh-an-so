package com.benhsoan.application.ucservice.vitalsign;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VitalSignAuthorizationService {

    private final CurrentUserPort currentUserPort;
    private final VisitRepository visitRepository;
    private final VitalSignAuthorizationAuditService authorizationAuditService;

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

    public UUID requireVisitReadAccess(UUID visitDoctorId, UUID visitId) {
        if (currentUserPort.hasRole("ADMIN")) {
            return currentUserPort.getCurrentUserId();
        }
        UUID currentUserId = currentUserPort.getCurrentUserId();
        if (!currentUserId.equals(visitDoctorId)) {
            authorizationAuditService.recordVisitAccessDenied(
                    currentUserId,
                    visitId,
                    "Bác sĩ chỉ có quyền xem chỉ số sinh tồn của lượt khám do mình phụ trách."
            );
            throw new MedicalRecordAccessDeniedException("Bác sĩ chỉ có quyền xem chỉ số sinh tồn của lượt khám do mình phụ trách.");
        }
        return currentUserId;
    }

    public UUID requirePatientHistoryReadAccess(UUID patientId) {
        if (currentUserPort.hasRole("ADMIN")) {
            return currentUserPort.getCurrentUserId();
        }
        UUID currentUserId = currentUserPort.getCurrentUserId();
        boolean hasAttended = visitRepository.findByPatientIdOrderByVisitAtDesc(patientId)
                .stream()
                .anyMatch(v -> currentUserId.equals(v.getDoctorId()));
        if (!hasAttended) {
            authorizationAuditService.recordPatientAccessDenied(
                    currentUserId,
                    patientId,
                    "Bác sĩ không có quyền xem lịch sử chỉ số sinh tồn của bệnh nhân không do mình phụ trách."
            );
            throw new MedicalRecordAccessDeniedException("Bác sĩ không có quyền xem lịch sử chỉ số sinh tồn của bệnh nhân không do mình phụ trách.");
        }
        return currentUserId;
    }
}

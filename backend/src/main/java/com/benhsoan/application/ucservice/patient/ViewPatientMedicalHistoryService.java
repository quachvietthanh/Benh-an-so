package com.benhsoan.application.ucservice.patient;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.patient.GetPatientMedicalHistoryQuery;
import com.benhsoan.port.dto.result.MedicalHistoryItemResult;
import com.benhsoan.port.inbound.patient.ViewPatientMedicalHistoryUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.queryRepository.patient.PatientMedicalHistoryQueryPort;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViewPatientMedicalHistoryService implements ViewPatientMedicalHistoryUseCase {

    private final PatientRepository patientRepository;
    private final PatientMedicalHistoryQueryPort patientMedicalHistoryQueryPort;
    private final MedicalRecordAccessAuditService medicalRecordAccessAuditService;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public Page<MedicalHistoryItemResult> viewMedicalHistory(GetPatientMedicalHistoryQuery query) {
        patientRepository.findById(query.patientId())
                .orElseThrow(() -> new PatientNotFoundException(query.patientId()));

        validatePermission();

        Page<MedicalHistoryItemResult> medicalHistory = patientMedicalHistoryQueryPort.findMedicalHistory(query);
        medicalRecordAccessAuditService.recordHistoryView(
                query.patientId(),
                currentUserPort.getCurrentUserId(),
                clockPort.now()
        );

        return medicalHistory;
    }

    private void validatePermission() {
        if (!currentUserPort.hasRole("ADMIN")
                && !currentUserPort.hasRole("DOCTOR")
                && !currentUserPort.hasRole("NURSE")) {
            throw new MedicalRecordAccessDeniedException();
        }
    }
}

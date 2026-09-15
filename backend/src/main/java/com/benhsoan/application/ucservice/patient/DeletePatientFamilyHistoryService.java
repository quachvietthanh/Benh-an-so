package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientFamilyHistory;
import com.benhsoan.domain.patient.exception.PatientFamilyHistoryNotFoundException;
import com.benhsoan.domain.patient.exception.PatientInactiveException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.patient.DeletePatientFamilyHistoryCommand;
import com.benhsoan.port.inbound.patient.DeletePatientFamilyHistoryUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientFamilyHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeletePatientFamilyHistoryService implements DeletePatientFamilyHistoryUseCase {

    private final PatientRepository patientRepository;
    private final PatientFamilyHistoryRepository patientFamilyHistoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public void deleteFamilyHistory(DeletePatientFamilyHistoryCommand command) {
        if (command == null || command.familyHistoryId() == null || command.patientId() == null) {
            throw new ValidationException("Family history ID and Patient ID are required.");
        }

        Patient patient = patientRepository.findById(command.patientId())
                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));
        if (!patient.isActive()) {
            throw new PatientInactiveException();
        }

        PatientFamilyHistory familyHistory = patientFamilyHistoryRepository.findById(command.familyHistoryId())
                .orElseThrow(() -> new PatientFamilyHistoryNotFoundException(command.familyHistoryId()));
        if (!familyHistory.getPatientId().equals(command.patientId()) || !familyHistory.isActive()) {
            throw new PatientFamilyHistoryNotFoundException(command.familyHistoryId());
        }

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        familyHistory.deactivate(currentUserId, now);
        patientFamilyHistoryRepository.save(familyHistory);

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.DELETE,
                ResourceType.PATIENT_FAMILY_HISTORY,
                familyHistory.getId(),
                command.reason(),
                null,
                now
        ));
    }
}

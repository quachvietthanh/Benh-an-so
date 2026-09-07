package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.domain.patient.PatientAllergyChangeLog;
import com.benhsoan.domain.patient.exception.PatientAllergyNotFoundException;
import com.benhsoan.domain.patient.exception.PatientInactiveException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.patient.DeletePatientAllergyCommand;
import com.benhsoan.port.inbound.patient.DeletePatientAllergyUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeletePatientAllergyService implements DeletePatientAllergyUseCase {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientAllergyChangeLogRepository changeLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final PatientAllergyChangeDetailBuilder changeDetailBuilder;

    @Override
    public void deleteAllergy(DeletePatientAllergyCommand command) {
        if (command == null || command.allergyId() == null || command.patientId() == null) {
            throw new ValidationException("Allergy ID and Patient ID are required.");
        }

        Patient patient = patientRepository.findById(command.patientId())
                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));

        if (!patient.isActive()) {
            throw new PatientInactiveException();
        }

        PatientAllergy allergy = patientAllergyRepository.findById(command.allergyId())
                .orElseThrow(() -> new PatientAllergyNotFoundException(command.allergyId()));

        if (!allergy.getPatientId().equals(command.patientId()) || !allergy.isActive()) {
            throw new PatientAllergyNotFoundException(command.allergyId());
        }

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        String beforeData = changeDetailBuilder.buildSnapshot(allergy);

        allergy.deactivate(currentUserId, now);
        patientAllergyRepository.save(allergy);

        PatientAllergyChangeLog changeLog = PatientAllergyChangeLog.create(
                allergy.getId(),
                command.patientId(),
                "DELETE",
                beforeData,
                null,
                command.reason(),
                currentUserId,
                now
        );
        changeLogRepository.save(changeLog);

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.DELETE,
                ResourceType.PATIENT,
                allergy.getId(),
                beforeData,
                null,
                now
        ));
    }
}

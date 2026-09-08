package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.domain.patient.PatientAllergyChangeLog;
import com.benhsoan.domain.patient.exception.PatientAllergyAlreadyExistsException;
import com.benhsoan.domain.patient.exception.PatientAllergyNotFoundException;
import com.benhsoan.domain.patient.exception.PatientInactiveException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.patient.UpdatePatientAllergyCommand;
import com.benhsoan.port.dto.result.patient.PatientAllergyResult;
import com.benhsoan.port.inbound.patient.UpdatePatientAllergyUseCase;
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
public class UpdatePatientAllergyService implements UpdatePatientAllergyUseCase {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final PatientAllergyChangeLogRepository changeLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final PatientAllResultMapper resultMapper;
    private final PatientAllergyChangeDetailBuilder changeDetailBuilder;

    @Override
    public PatientAllergyResult updateAllergy(UpdatePatientAllergyCommand command) {
        if (command == null || command.allergyId() == null || command.patientId() == null) {
            throw new ValidationException("Allergy ID and Patient ID are required.");
        }
        if (command.allergenName() == null || command.allergenName().isBlank()) {
            throw new ValidationException("Tên hoạt chất hoặc nhóm thuốc không được để trống.");
        }
        if (command.severity() == null) {
            throw new ValidationException("Mức độ phản ứng không được để trống.");
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

        String normalized = PatientAllergy.normalizeAllergenName(command.allergenName());
        if (patientAllergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrueAndIdNot(
                command.patientId(),
                normalized,
                command.allergyId()
        )) {
            throw new PatientAllergyAlreadyExistsException(command.allergenName());
        }

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        String beforeData = changeDetailBuilder.buildSnapshot(allergy);

        allergy.update(
                command.allergenName(),
                command.severity(),
                command.reaction(),
                command.notes(),
                currentUserId,
                now
        );

        PatientAllergy updated;
        try {
            updated = patientAllergyRepository.save(allergy);
        } catch (DataIntegrityViolationException e) {
            throw new PatientAllergyAlreadyExistsException(command.allergenName());
        }
        String afterData = changeDetailBuilder.buildSnapshot(updated);

        PatientAllergyChangeLog changeLog = PatientAllergyChangeLog.create(
                updated.getId(),
                command.patientId(),
                "UPDATE",
                beforeData,
                afterData,
                command.changeReason(),
                currentUserId,
                now
        );
        changeLogRepository.save(changeLog);

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.UPDATE,
                ResourceType.PATIENT_ALLERGY,
                updated.getId(),
                afterData,
                null,
                now
        ));

        return resultMapper.toResult(updated);
    }
}

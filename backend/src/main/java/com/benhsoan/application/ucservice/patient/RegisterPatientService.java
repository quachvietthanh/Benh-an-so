package com.benhsoan.application.ucservice.patient;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.enums.PatientChangeAction;
import com.benhsoan.domain.patient.exception.PatientAlreadyExistsException;
import com.benhsoan.port.dto.command.patient.RegisterPatientCommand;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.RegisterPatientUseCase;
import com.benhsoan.port.outbound.generator.PatientCodeGenerator;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RegisterPatientService
        implements RegisterPatientUseCase {

    private final PatientRepository patientRepository;

    private final PatientChangeLogRepository patientChangeLogRepository;

    private final PatientCodeGenerator patientCodeGenerator;

    private final CurrentUserPort currentUserPort;

    private final PatientChangeDetailBuilder changeDetailBuilder;

    private final PatientResultMapper patientResultMapper;

    private final AuditLogRepository auditLogRepository;

    @Override
    public PatientResult register(RegisterPatientCommand command) {

        validate(command);

        UUID currentUserId =
                currentUserPort.getCurrentUserId();

        String patientCode =
                patientCodeGenerator.generate();

        Patient patient =
                Patient.create(
                        patientCode,
                        command.fullName(),
                        command.dateOfBirth(),
                        command.gender(),
                        normalizePhone(command.phone()),
                        command.email(),
                        command.address(),
                        command.identityNumber(),
                        command.insuranceNumber(),
                        command.bloodType(),
                        command.emergencyContact(),
                        command.emergencyPhone(),
                        currentUserId
                );

        Patient saved =
                patientRepository.save(patient);

        String changeDetail = changeDetailBuilder.forCreate(saved);

        PatientChangeLog log =
                PatientChangeLog.create(
                        saved.getId(),
                        currentUserId,
                        PatientChangeAction.CREATE,
                        changeDetail
                );

        patientChangeLogRepository.save(log);

        auditLogRepository.save(
                AuditLog.create(
                        currentUserId,
                        ActionType.CREATE,
                        ResourceType.PATIENT,
                        saved.getId(),
                        """
                        {
                        "patientCode":"%s",
                        "fullName":"%s"
                        }
                        """
                        .formatted( patient.getPatientCode(), patient.getFullName()),
                        null
                )
        );

        return patientResultMapper.toResult(patient);
    }

    private void validate(RegisterPatientCommand command) {

        if (command.identityNumber() != null
                && patientRepository.existsByIdentityNumber(
                        command.identityNumber())) {

            throw new PatientAlreadyExistsException(
                    "identity number"
            );
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }

        String trimmed = phone.trim();

        return trimmed.startsWith("+84")
                ? "0" + trimmed.substring(3)
                : trimmed;
    }
}

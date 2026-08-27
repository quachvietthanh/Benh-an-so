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
import com.benhsoan.domain.patient.exception.PatientConsentRequiredException;
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

        boolean consentAgreed = Boolean.TRUE.equals(command.consentAgreed());

        String identityNumber = normalizeIdentityNumber(command.identityNumber());

        Patient patient =
                Patient.create(
                        patientCode,
                        command.fullName(),
                        command.dateOfBirth(),
                        command.gender(),
                        normalizePhone(command.phone()),
                        command.email(),
                        command.address(),
                        identityNumber,
                        command.insuranceNumber(),
                        command.bloodType(),
                        command.emergencyContact(),
                        command.emergencyPhone(),
                        consentAgreed,
                        command.consentVersion(),
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
                        "fullName":"%s",
                        "consentAgreed":%s,
                        "consentVersion":"%s"
                        }
                        """
                        .formatted(saved.getPatientCode(), saved.getFullName(), saved.isConsentAgreed(), saved.getConsentVersion()),
                        null
                )
        );

        return patientResultMapper.toResult(saved);
    }

    private void validate(RegisterPatientCommand command) {

        // QTN-24: Phải có phiếu đồng ý trước khi xử lý dữ liệu cá nhân
        if (command.consentAgreed() == null || !command.consentAgreed()) {
            throw new PatientConsentRequiredException("Phải có phiếu đồng ý trước khi xử lý dữ liệu cá nhân (QTN-24).");
        }

        String identityNumber = normalizeIdentityNumber(command.identityNumber());

        if (identityNumber != null
                && patientRepository.existsByIdentityNumber(identityNumber)) {

            throw new PatientAlreadyExistsException(
                    "identity number"
            );
        }
    }

    private String normalizeIdentityNumber(String identityNumber) {
        if (identityNumber == null || identityNumber.isBlank()) {
            return null;
        }
        return identityNumber.trim();
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

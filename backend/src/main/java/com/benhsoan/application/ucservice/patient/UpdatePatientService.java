package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
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
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.patient.UpdatePatientCommand;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.UpdatePatientUseCase;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdatePatientService
        implements UpdatePatientUseCase {

    private final PatientRepository patientRepository;

    private final PatientChangeLogRepository patientChangeLogRepository;

    private final CurrentUserPort currentUserPort;

    private final PatientResultMapper patientResultMapper;

    private final PatientChangeDetailBuilder changeDetailBuilder;

    private final AuditLogRepository auditLogRepository;

    @Override
    public PatientResult update( UUID patientId, UpdatePatientCommand command ) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        validate(
                patientId,
                command
        );

        Patient oldPatient = Patient.restore(
                patient.getId(),
                patient.getPatientCode(),
                patient.getFullName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getIdentityNumber(),
                patient.getInsuranceNumber(),
                patient.getBloodType(),
                patient.getEmergencyContact(),
                patient.getEmergencyPhone(),
                patient.isActive(),
                patient.getCreatedAt(),
                patient.getUpdatedAt(),
                patient.getUserId(),
                patient.getCreatedBy(),
                patient.isConsentAgreed(),
                patient.getConsentAgreedAt(),
                patient.getConsentVersion(),
                patient.isConsentWithdrawn(),
                patient.getConsentWithdrawnAt(),
                patient.getConsentWithdrawnReason(),
                patient.isNonMedicalUseRestricted()
        );

        UUID currentUserId =
                currentUserPort.getCurrentUserId();

        patient.updateProfile(
                command.fullName(),
                command.dateOfBirth(),
                command.gender(),
                command.phone(),
                command.email(),
                command.address(),
                command.identityNumber(),
                command.insuranceNumber(),
                command.bloodType(),
                command.emergencyContact(),
                command.emergencyPhone()
        );

        if (command.active() && !patient.isActive()) {
            patient.activate();
        }

        if (!command.active() && patient.isActive()) {
            patient.deactivate();
        }

        // Handle consent withdrawal (NCL-15-CN-001-TC-03) or renewal
        if (Boolean.TRUE.equals(command.consentWithdrawn()) && !patient.isConsentWithdrawn()) {
            patient.withdrawConsent(command.consentWithdrawnReason(), Instant.now());
        } else if (Boolean.FALSE.equals(command.consentWithdrawn()) && patient.isConsentWithdrawn()) {
            patient.renewConsent(command.consentVersion(), Instant.now());
        } else if (Boolean.TRUE.equals(command.consentAgreed()) && !patient.isConsentAgreed()) {
            patient.renewConsent(command.consentVersion(), Instant.now());
        }

        String detail = changeDetailBuilder.forUpdate( oldPatient, patient );

        Patient updatedPatient =
                patientRepository.save(patient);

        PatientChangeLog log =
        PatientChangeLog.create(
                updatedPatient.getId(),
                currentUserId,
                PatientChangeAction.UPDATE,
                detail
        );
        patientChangeLogRepository.save(log);

        auditLogRepository.save(
                AuditLog.create(
                        currentUserId,
                        ActionType.UPDATE,
                        ResourceType.PATIENT,
                        updatedPatient.getId(),
                        """
                        {
                        "patientCode":"%s",
                        "fullName":"%s",
                        "consentWithdrawn":%s,
                        "nonMedicalUseRestricted":%s
                        }
                        """
                        .formatted(
                                patient.getPatientCode(),
                                patient.getFullName(),
                                patient.isConsentWithdrawn(),
                                patient.isNonMedicalUseRestricted()
                        ),
                        null
                )
        );

        return patientResultMapper.toResult(updatedPatient);
    }

    private void validate(
            UUID patientId,
            UpdatePatientCommand command
    ) {

        String identityNumber =
                command.identityNumber();

        if (identityNumber != null
                && patientRepository.existsByIdentityNumberAndIdNot(
                        identityNumber,
                        patientId
                )) {

            throw new PatientAlreadyExistsException(
                    "Identity number"
            );
        }
    }
}
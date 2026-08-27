package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.PatientConsentVersion;
import com.benhsoan.domain.patient.enums.PatientChangeAction;
import com.benhsoan.domain.patient.exception.PatientAlreadyExistsException;
import com.benhsoan.domain.patient.exception.PatientConsentAccessDeniedException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.patient.UpdatePatientCommand;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.UpdatePatientUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
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

        Patient patient = patientRepository.findByIdForUpdate(patientId)
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

        if (command.fullName() != null) {
            patient.updateProfile(
                    command.fullName(),
                    command.dateOfBirth(),
                    command.gender(),
                    command.phone(),
                    command.email(),
                    command.address(),
                    normalizeIdentityNumber(command.identityNumber()),
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
        }

        // Handle consent withdrawal (NCL-15-CN-001-TC-03) or renewal
        boolean isChangingWithdrawal = command.consentWithdrawn() != null
                && command.consentWithdrawn() != patient.isConsentWithdrawn();
        boolean isChangingAgreement = command.consentAgreed() != null
                && command.consentAgreed() != patient.isConsentAgreed();
        boolean isChangingVersion = command.consentVersion() != null
                && !Objects.equals(command.consentVersion(), patient.getConsentVersion());
        boolean isChangingWithdrawReason = command.consentWithdrawnReason() != null
                && !Objects.equals(command.consentWithdrawnReason(), patient.getConsentWithdrawnReason());

        boolean isModifyingConsent = isChangingWithdrawal
                || isChangingAgreement
                || isChangingVersion
                || isChangingWithdrawReason;

        if (isModifyingConsent) {
            if (!currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")) {
                throw new PatientConsentAccessDeniedException();
            }
        }

        if (Boolean.FALSE.equals(command.consentAgreed())
                && !Boolean.TRUE.equals(command.consentWithdrawn())) {
            throw new ValidationException(
                    "consentAgreed=false requires consentWithdrawn=true to withdraw consent."
            );
        }

        if (Boolean.TRUE.equals(command.consentWithdrawn())) {
            if (!patient.isConsentWithdrawn()) {
                patient.withdrawConsent(command.consentWithdrawnReason(), Instant.now());
            } else if (command.consentWithdrawnReason() != null
                    && !Objects.equals(command.consentWithdrawnReason(), patient.getConsentWithdrawnReason())) {
                patient.withdrawConsent(command.consentWithdrawnReason(), patient.getConsentWithdrawnAt());
            }
        } else if (Boolean.FALSE.equals(command.consentWithdrawn()) && patient.isConsentWithdrawn()) {
            if (!Boolean.TRUE.equals(command.consentAgreed())) {
                throw new ValidationException(
                        "Phải ghi nhận sự đồng ý mới trước khi gia hạn xử lý dữ liệu cá nhân (QTN-24)."
                );
            }
            patient.renewConsent(PatientConsentVersion.requireSupported(command.consentVersion()), Instant.now());
        } else if (Boolean.TRUE.equals(command.consentAgreed()) && !patient.isConsentAgreed()) {
            patient.renewConsent(PatientConsentVersion.requireSupported(command.consentVersion()), Instant.now());
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
                        "consentAgreed":%s,
                        "consentVersion":"%s",
                        "consentWithdrawn":%s,
                        "nonMedicalUseRestricted":%s
                        }
                        """
                        .formatted(
                                patient.getPatientCode(),
                                patient.getFullName(),
                                patient.isConsentAgreed(),
                                patient.getConsentVersion(),
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
                normalizeIdentityNumber(command.identityNumber());

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

    private String normalizeIdentityNumber(String identityNumber) {
        if (identityNumber == null || identityNumber.isBlank()) {
            return null;
        }
        return identityNumber.trim();
    }

}

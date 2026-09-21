package com.benhsoan.persistence.mapper.patient;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.persistence.entity.patient.PatientEntity;

@Component
public class PatientPersistenceMapper {

    public Patient toDomain(PatientEntity entity) {

        if (entity == null) {
            return null;
        }

        Patient patient = Patient.restore(
                entity.getId(),
                entity.getPatientCode(),
                entity.getFullName(),
                entity.getDateOfBirth(),
                entity.getGender(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddress(),
                entity.getIdentityNumber(),
                entity.getInsuranceNumber(),
                entity.getBloodType(),
                entity.getEmergencyContact(),
                entity.getEmergencyRelationship(),
                entity.getEmergencyPhone(),
                entity.getGuardianName(),
                entity.getGuardianRelationship(),
                entity.getGuardianPhone(),
                entity.getGuardianIdentityNumber(),
                entity.getGuardianUserId(),
                entity.getConsentSignerName(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUserId(),
                entity.getCreatedBy(),
                entity.isConsentAgreed(),
                entity.getConsentAgreedAt(),
                entity.getConsentVersion(),
                entity.isConsentWithdrawn(),
                entity.getConsentWithdrawnAt(),
                entity.getConsentWithdrawnReason(),
                entity.isNonMedicalUseRestricted(),
                entity.getStatus(),
                entity.getMergedIntoPatientId(),
                entity.getMergedAt(),
                entity.getMergedBy(),
                entity.getMergeReason()
        );
        patient.changePregnancyStatus(entity.getPregnancyStatus());
        return patient;
    }

    public PatientEntity toEntity(Patient domain) {

        if (domain == null) {
            return null;
        }

        return PatientEntity.builder()
                .id(domain.getId())
                .patientCode(domain.getPatientCode())
                .fullName(domain.getFullName())
                .dateOfBirth(domain.getDateOfBirth())
                .gender(domain.getGender())
                .phone(domain.getPhone())
                .email(domain.getEmail())
                .address(domain.getAddress())
                .identityNumber(domain.getIdentityNumber())
                .insuranceNumber(domain.getInsuranceNumber())
                .bloodType(domain.getBloodType())
                .emergencyContact(domain.getEmergencyContact())
                .emergencyRelationship(domain.getEmergencyRelationship())
                .emergencyPhone(domain.getEmergencyPhone())
                .guardianName(domain.getGuardianName())
                .guardianRelationship(domain.getGuardianRelationship())
                .guardianPhone(domain.getGuardianPhone())
                .guardianIdentityNumber(domain.getGuardianIdentityNumber())
                .guardianUserId(domain.getGuardianUserId())
                .consentSignerName(domain.getConsentSignerName())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .userId(domain.getUserId())
                .createdBy(domain.getCreatedBy())
                .consentAgreed(domain.isConsentAgreed())
                .consentAgreedAt(domain.getConsentAgreedAt())
                .consentVersion(domain.getConsentVersion())
                .consentWithdrawn(domain.isConsentWithdrawn())
                .consentWithdrawnAt(domain.getConsentWithdrawnAt())
                .consentWithdrawnReason(domain.getConsentWithdrawnReason())
                .nonMedicalUseRestricted(domain.isNonMedicalUseRestricted())
                .status(domain.getStatus())
                .mergedIntoPatientId(domain.getMergedIntoPatientId())
                .mergedAt(domain.getMergedAt())
                .mergedBy(domain.getMergedBy())
                .mergeReason(domain.getMergeReason())
                .pregnancyStatus(domain.getPregnancyStatus())
                .build();
    }
}

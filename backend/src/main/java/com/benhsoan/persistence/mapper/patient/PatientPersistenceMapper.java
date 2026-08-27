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

        return Patient.restore(
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
                entity.getEmergencyPhone(),
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
                entity.isNonMedicalUseRestricted()
        );
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
                .emergencyPhone(domain.getEmergencyPhone())
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
                .build();
    }
}

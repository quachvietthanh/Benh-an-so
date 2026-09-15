package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;

public record PatientResult(

        UUID id,

        String patientCode,

        String fullName,

        LocalDate dateOfBirth,

        Gender gender,

        String phone,

        String email,

        String address,

        String identityNumber,

        String insuranceNumber,

        BloodType bloodType,

        String emergencyContact,

        String emergencyRelationship,

        String emergencyPhone,

        String guardianName,

        String guardianRelationship,

        String guardianPhone,

        String guardianIdentityNumber,

        UUID guardianUserId,

        String consentSignerName,

        boolean isMinor,

        boolean requiresAdultTransitionPrompt,

        boolean active,

        Instant createdAt,

        Instant updatedAt,

        boolean consentAgreed,

        Instant consentAgreedAt,

        String consentVersion,

        boolean consentWithdrawn,

        Instant consentWithdrawnAt,

        String consentWithdrawnReason,

        boolean nonMedicalUseRestricted

) {
    public PatientResult(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyRelationship,
            String emergencyPhone,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            boolean consentAgreed,
            Instant consentAgreedAt,
            String consentVersion,
            boolean consentWithdrawn,
            Instant consentWithdrawnAt,
            String consentWithdrawnReason,
            boolean nonMedicalUseRestricted
    ) {
        this(
                id,
                patientCode,
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                emergencyRelationship,
                emergencyPhone,
                null,
                null,
                null,
                null,
                null,
                fullName,
                false,
                false,
                active,
                createdAt,
                updatedAt,
                consentAgreed,
                consentAgreedAt,
                consentVersion,
                consentWithdrawn,
                consentWithdrawnAt,
                consentWithdrawnReason,
                nonMedicalUseRestricted
        );
    }
    public PatientResult(
            UUID id,
            String patientCode,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String phone,
            String email,
            String address,
            String identityNumber,
            String insuranceNumber,
            BloodType bloodType,
            String emergencyContact,
            String emergencyPhone,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            boolean consentAgreed,
            Instant consentAgreedAt,
            String consentVersion,
            boolean consentWithdrawn,
            Instant consentWithdrawnAt,
            String consentWithdrawnReason,
            boolean nonMedicalUseRestricted
    ) {
        this(
                id,
                patientCode,
                fullName,
                dateOfBirth,
                gender,
                phone,
                email,
                address,
                identityNumber,
                insuranceNumber,
                bloodType,
                emergencyContact,
                null,
                emergencyPhone,
                active,
                createdAt,
                updatedAt,
                consentAgreed,
                consentAgreedAt,
                consentVersion,
                consentWithdrawn,
                consentWithdrawnAt,
                consentWithdrawnReason,
                nonMedicalUseRestricted
        );
    }
}
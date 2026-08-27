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
}
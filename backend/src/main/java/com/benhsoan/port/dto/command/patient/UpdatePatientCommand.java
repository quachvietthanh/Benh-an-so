package com.benhsoan.port.dto.command.patient;

import java.time.LocalDate;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;

import lombok.Builder;

@Builder
public record UpdatePatientCommand(

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

        Boolean consentAgreed,

        Boolean consentWithdrawn,

        String consentWithdrawnReason,

        String consentVersion

) {
}
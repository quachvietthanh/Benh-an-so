package com.benhsoan.port.dto.command.auth;

import java.time.LocalDate;

import com.benhsoan.domain.patient.enums.Gender;

public record PatientPortalRegistrationCommand(

        String phone,

        String password,

        String fullName,

        LocalDate dateOfBirth,

        Gender gender,

        String identityNumber,

        String email

) {
}

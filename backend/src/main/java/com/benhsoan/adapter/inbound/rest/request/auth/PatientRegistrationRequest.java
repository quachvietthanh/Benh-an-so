package com.benhsoan.adapter.inbound.rest.request.auth;

import java.time.LocalDate;

import com.benhsoan.domain.patient.enums.Gender;

import jakarta.validation.constraints.NotBlank;

public record PatientRegistrationRequest(

        @NotBlank
        String phone,

        @NotBlank
        String password,

        @NotBlank
        String fullName,

        LocalDate dateOfBirth,

        Gender gender,

        String identityNumber

) {
}

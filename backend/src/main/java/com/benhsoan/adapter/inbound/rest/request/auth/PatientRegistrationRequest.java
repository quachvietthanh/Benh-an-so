package com.benhsoan.adapter.inbound.rest.request.auth;

import java.time.LocalDate;

import com.benhsoan.domain.patient.enums.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientRegistrationRequest(

        @NotBlank
        String phone,

        @NotBlank
        @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự.")
        String password,

        @NotBlank
        String fullName,

        LocalDate dateOfBirth,

        Gender gender,

        String identityNumber,

        @Email(message = "Email không đúng định dạng.")
        String email

) {
}

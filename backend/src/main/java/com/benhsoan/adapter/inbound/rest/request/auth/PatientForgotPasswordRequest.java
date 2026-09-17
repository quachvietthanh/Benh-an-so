package com.benhsoan.adapter.inbound.rest.request.auth;

import jakarta.validation.constraints.NotBlank;

public record PatientForgotPasswordRequest(

        @NotBlank(message = "Số điện thoại không được để trống.")
        String phone

) {
}

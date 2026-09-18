package com.benhsoan.adapter.inbound.rest.request.auth;

import jakarta.validation.constraints.NotBlank;

public record PatientVerifyRecoveryCodeRequest(

        @NotBlank(message = "Số điện thoại không được để trống.")
        String phone,

        @NotBlank(message = "Mã xác thực không được để trống.")
        String code

) {
}

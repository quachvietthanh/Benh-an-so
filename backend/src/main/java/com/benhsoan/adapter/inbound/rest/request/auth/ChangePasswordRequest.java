package com.benhsoan.adapter.inbound.rest.request.auth;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(

        @NotBlank(message = "Mật khẩu cũ không được để trống.")
        String oldPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống.")
        String newPassword

) {
}

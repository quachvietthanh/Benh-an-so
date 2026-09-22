package com.benhsoan.port.dto.command.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyTwoFactorCommand(

        @NotBlank
        String twoFactorToken,

        @NotBlank
        String code,

        String ipAddress

) {
    public VerifyTwoFactorCommand(String twoFactorToken, String code) {
        this(twoFactorToken, code, null);
    }
}

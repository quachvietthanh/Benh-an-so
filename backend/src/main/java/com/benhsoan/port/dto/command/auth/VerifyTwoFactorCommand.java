package com.benhsoan.port.dto.command.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyTwoFactorCommand(

        @NotBlank
        String twoFactorToken,

        @NotBlank
        String code,

        String ipAddress,

        String userAgent

) {
    public VerifyTwoFactorCommand(String twoFactorToken, String code) {
        this(twoFactorToken, code, null, null);
    }

    public VerifyTwoFactorCommand(String twoFactorToken, String code, String ipAddress) {
        this(twoFactorToken, code, ipAddress, null);
    }
}

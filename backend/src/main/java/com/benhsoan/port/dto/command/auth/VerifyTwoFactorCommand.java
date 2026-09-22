package com.benhsoan.port.dto.command.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyTwoFactorCommand(

        @NotBlank
        String twoFactorToken,

        @NotBlank
        String code

) {}

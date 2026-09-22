package com.benhsoan.port.dto.command.auth;

import jakarta.validation.constraints.NotBlank;

public record ResendTwoFactorCommand(

        @NotBlank
        String twoFactorToken

) {}

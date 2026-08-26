package com.benhsoan.port.dto.command.auth;

import jakarta.validation.constraints.NotBlank;

public record PatientLoginCommand(

        @NotBlank
        String phone,

        @NotBlank
        String password,

        String ipAddress,

        String userAgent

) {
}

package com.benhsoan.port.dto.command.auth;

public record PatientForgotPasswordCommand(
        String phone,
        String ipAddress,
        String userAgent
) {
}

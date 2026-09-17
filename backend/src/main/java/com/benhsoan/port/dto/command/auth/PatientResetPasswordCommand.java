package com.benhsoan.port.dto.command.auth;

public record PatientResetPasswordCommand(
        String phone,
        String code,
        String newPassword,
        String ipAddress,
        String userAgent
) {
}

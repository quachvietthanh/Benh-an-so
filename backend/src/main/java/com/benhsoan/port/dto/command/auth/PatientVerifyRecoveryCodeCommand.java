package com.benhsoan.port.dto.command.auth;

public record PatientVerifyRecoveryCodeCommand(
        String phone,
        String code
) {
}

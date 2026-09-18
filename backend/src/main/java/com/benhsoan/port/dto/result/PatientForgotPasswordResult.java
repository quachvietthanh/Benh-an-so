package com.benhsoan.port.dto.result;

public record PatientForgotPasswordResult(
        String message,
        long expiresInSeconds
) {
}

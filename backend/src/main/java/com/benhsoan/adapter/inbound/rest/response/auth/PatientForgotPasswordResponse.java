package com.benhsoan.adapter.inbound.rest.response.auth;

public record PatientForgotPasswordResponse(
        String message,
        long expiresInSeconds
) {
}

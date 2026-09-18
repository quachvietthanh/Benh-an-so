package com.benhsoan.adapter.inbound.rest.response.auth;

public record PatientVerifyRecoveryCodeResponse(
        boolean valid,
        String message
) {
}

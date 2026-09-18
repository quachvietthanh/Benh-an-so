package com.benhsoan.adapter.inbound.rest.response.user;

import java.time.Instant;
import java.util.UUID;

public record ResetPasswordResponse(

        UUID userId,

        String username,

        String temporaryPassword,

        Instant resetAt,

        Instant tempPasswordExpiresAt

) {
    public ResetPasswordResponse(UUID userId, String username, String temporaryPassword, Instant resetAt) {
        this(userId, username, temporaryPassword, resetAt, null);
    }
}


package com.benhsoan.adapter.inbound.rest.response.session;

import java.time.Instant;
import java.util.UUID;

public record SessionStatusResponse(
        UUID sessionId,
        UUID userId,
        String username,
        String fullName,
        String role,
        Instant createdAt,
        Instant lastActivityAt,
        Instant expiresAt,
        Instant warningAt,
        Instant refreshExpiresAt,
        long inactivityTimeoutSeconds,
        boolean revoked
) {
}
package com.benhsoan.port.dto.result.session;

import java.time.Instant;
import java.util.UUID;

public record SessionStatusResult(
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
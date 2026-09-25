package com.benhsoan.port.dto.result.auth;

import java.time.Instant;
import java.util.UUID;

public record ExtendSessionResult(
        UUID sessionId,
        Instant lastUsedAt,
        Instant idleExpiresAt,
        String message
) {}

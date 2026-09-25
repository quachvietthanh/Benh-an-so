package com.benhsoan.adapter.inbound.rest.response.auth;

import java.time.Instant;
import java.util.UUID;

public record ExtendSessionResponse(
        UUID sessionId,
        Instant lastUsedAt,
        Instant idleExpiresAt,
        String message
) {}

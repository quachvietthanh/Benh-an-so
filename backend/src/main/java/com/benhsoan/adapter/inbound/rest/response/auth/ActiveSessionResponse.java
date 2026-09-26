package com.benhsoan.adapter.inbound.rest.response.auth;

import java.time.Instant;
import java.util.UUID;

public record ActiveSessionResponse(
        UUID sessionId,
        UUID userId,
        String username,
        String fullName,
        String roleName,
        String ipAddress,
        String userAgent,
        Instant createdAt,
        Instant lastUsedAt,
        boolean isCurrentSession
) {}

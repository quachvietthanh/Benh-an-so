package com.benhsoan.port.dto.result.auth;

import java.time.Instant;
import java.util.UUID;

public record ActiveSessionResult(
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

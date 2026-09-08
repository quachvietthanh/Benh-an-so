package com.benhsoan.port.dto.result.security;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.security.enums.AlertSeverity;
import com.benhsoan.domain.security.enums.AlertStatus;
import com.benhsoan.domain.security.enums.AlertType;

public record SecurityAlertResult(
        UUID id,
        UUID userId,
        String username,
        String fullName,
        AlertType alertType,
        AlertSeverity severity,
        String description,
        int accessCount,
        Instant windowStart,
        Instant windowEnd,
        AlertStatus status,
        Instant createdAt
) {
}

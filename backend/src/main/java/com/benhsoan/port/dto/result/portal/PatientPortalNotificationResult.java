package com.benhsoan.port.dto.result.portal;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;

public record PatientPortalNotificationResult(
        UUID id,
        PatientPortalNotificationType type,
        String title,
        String message,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}

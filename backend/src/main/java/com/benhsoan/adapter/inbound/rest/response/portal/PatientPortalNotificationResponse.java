package com.benhsoan.adapter.inbound.rest.response.portal;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.portal.notification.PatientPortalNotificationType;

public record PatientPortalNotificationResponse(
        UUID id,
        PatientPortalNotificationType type,
        String title,
        String message,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}

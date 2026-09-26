package com.benhsoan.adapter.inbound.rest.response.auditlog;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;

/**
 * Read-only view of an administrative operation log entry.
 * {@code detail} is a JSON object of the form
 * {@code {"before": {...}, "after": {...}}}.
 */
public record AdminOperationLogResponse(
        UUID id,
        UUID actorId,
        String actorName,
        ActionType actionType,
        ResourceType resourceType,
        UUID resourceId,
        String detail,
        Instant createdAt
) {
}

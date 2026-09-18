package com.benhsoan.port.dto.command.auditlog;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.auditlog.enums.ResourceType;

/**
 * Filter criteria for the administrative operation log (TC-02). All fields are
 * optional; when present they are AND-ed together.
 */
public record AdminOperationLogQuery(
        UUID actorId,
        ResourceType resourceType,
        Instant from,
        Instant to
) {
}

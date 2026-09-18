package com.benhsoan.port.dto.result.auditlog;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;

public record AdminOperationLogResult(
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

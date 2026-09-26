package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;

public record LoginAuditLogResult(
        UUID id,
        UUID userId,
        ActionType actionType,
        ResourceType resourceType,
        UUID resourceId,
        String detail,
        String ipAddress,
        Instant createdAt
) {
}

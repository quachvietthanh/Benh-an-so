package com.benhsoan.port.dto.command.auditlog;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;

import lombok.Builder;


@Builder
public record SearchAuditLogCommand(

        UUID userId,

        ActionType actionType,

        ResourceType resourceType,

        UUID resourceId,

        Instant fromCreatedAt,

        Instant toCreatedAt,

        Pageable pageable

) {
}

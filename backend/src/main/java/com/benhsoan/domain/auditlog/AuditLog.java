package com.benhsoan.domain.auditlog;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.shared.Guard.Guard;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    private UUID id;

    private UUID userId;

    private ActionType actionType;

    private ResourceType resourceType;

    private UUID resourceId;

    private String detail;

    private String ipAddress;

    private Instant createdAt;

    private AuditLog(
            UUID id,
            UUID userId,
            ActionType actionType,
            ResourceType resourceType,
            UUID resourceId,
            String detail,
            String ipAddress,
            Instant createdAt
    ) {
        this.id = Guard.require(id, "Audit log id");
        this.userId = Guard.require(userId, "User id");
        this.actionType = Guard.require(actionType, "Action type");
        this.resourceType = Guard.require(resourceType, "Resource type");
        this.createdAt = Guard.require(createdAt, "Created at");

        this.resourceId = resourceId;
        this.detail = detail;
        this.ipAddress = ipAddress;
    }

    public static AuditLog create(
            UUID userId,
            ActionType actionType,
            ResourceType resourceType,
            UUID resourceId,
            String detail,
            String ipAddress
    ) {
        return create(userId, actionType, resourceType, resourceId, detail, ipAddress, Instant.now());
    }

    public static AuditLog create(
            UUID userId,
            ActionType actionType,
            ResourceType resourceType,
            UUID resourceId,
            String detail,
            String ipAddress,
            Instant createdAt
    ) {
        return new AuditLog(
                UUID.randomUUID(),
                userId,
                actionType,
                resourceType,
                resourceId,
                detail,
                ipAddress,
                createdAt
        );
    }

    public static AuditLog restore(
            UUID id,
            UUID userId,
            ActionType actionType,
            ResourceType resourceType,
            UUID resourceId,
            String detail,
            String ipAddress,
            Instant createdAt
    ) {
        return new AuditLog(
                id,
                userId,
                actionType,
                resourceType,
                resourceId,
                detail,
                ipAddress,
                createdAt
        );
    }
}

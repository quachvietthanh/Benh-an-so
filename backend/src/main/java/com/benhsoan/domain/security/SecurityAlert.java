package com.benhsoan.domain.security;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.security.enums.AlertSeverity;
import com.benhsoan.domain.security.enums.AlertStatus;
import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityAlert {

    private UUID id;
    private UUID userId;
    private AlertType alertType;
    private AlertSeverity severity;
    private String description;
    private int accessCount;
    private Instant windowStart;
    private Instant windowEnd;
    private AlertStatus status;
    private Instant createdAt;

    private SecurityAlert(
            UUID id,
            UUID userId,
            AlertType alertType,
            AlertSeverity severity,
            String description,
            int accessCount,
            Instant windowStart,
            Instant windowEnd,
            AlertStatus status,
            Instant createdAt
    ) {
        this.id = Guard.require(id, "Security alert id");
        this.userId = Guard.require(userId, "User id");
        this.alertType = Guard.require(alertType, "Alert type");
        this.severity = Guard.require(severity, "Severity");
        this.description = Guard.require(description, "Description");
        if (accessCount < 0) {
            throw new ValidationException("Access count cannot be negative.");
        }
        this.accessCount = accessCount;
        this.windowStart = Guard.require(windowStart, "Window start");
        this.windowEnd = Guard.require(windowEnd, "Window end");
        this.status = Guard.require(status, "Status");
        this.createdAt = Guard.require(createdAt, "Created at");
    }

    public static SecurityAlert create(
            UUID userId,
            AlertType alertType,
            AlertSeverity severity,
            String description,
            int accessCount,
            Instant windowStart,
            Instant windowEnd,
            Instant createdAt
    ) {
        return new SecurityAlert(
                UUID.randomUUID(),
                userId,
                alertType,
                severity,
                description,
                accessCount,
                windowStart,
                windowEnd,
                AlertStatus.UNREAD,
                createdAt
        );
    }

    public static SecurityAlert restore(
            UUID id,
            UUID userId,
            AlertType alertType,
            AlertSeverity severity,
            String description,
            int accessCount,
            Instant windowStart,
            Instant windowEnd,
            AlertStatus status,
            Instant createdAt
    ) {
        return new SecurityAlert(
                id,
                userId,
                alertType,
                severity,
                description,
                accessCount,
                windowStart,
                windowEnd,
                status,
                createdAt
        );
    }

    public void updateDetection(int accessCount, String description) {
        if (accessCount < 0) {
            throw new ValidationException("Access count cannot be negative.");
        }
        this.accessCount = accessCount;
        this.description = Guard.require(description, "Description");
    }
}

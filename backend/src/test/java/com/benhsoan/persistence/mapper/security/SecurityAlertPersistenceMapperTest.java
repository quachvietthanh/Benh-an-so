package com.benhsoan.persistence.mapper.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertSeverity;
import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.persistence.entity.security.SecurityAlertEntity;

class SecurityAlertPersistenceMapperTest {

    private final SecurityAlertPersistenceMapper mapper = new SecurityAlertPersistenceMapper();

    @Test
    void roundTripsDomainAndEntity() {
        SecurityAlert alert = SecurityAlert.create(
                UUID.randomUUID(),
                AlertType.THRESHOLD_EXCEEDED,
                AlertSeverity.HIGH,
                "Threshold exceeded",
                21,
                Instant.parse("2026-08-11T10:00:00Z"),
                Instant.parse("2026-08-11T11:00:00Z"),
                Instant.parse("2026-08-11T10:05:00Z")
        );

        SecurityAlertEntity entity = mapper.toEntity(alert);
        SecurityAlert restored = mapper.toDomain(entity);

        assertEquals(alert.getId(), restored.getId());
        assertEquals(alert.getUserId(), restored.getUserId());
        assertEquals(alert.getAlertType(), restored.getAlertType());
        assertEquals(alert.getSeverity(), restored.getSeverity());
        assertEquals(alert.getDescription(), restored.getDescription());
        assertEquals(alert.getAccessCount(), restored.getAccessCount());
        assertEquals(alert.getWindowStart(), restored.getWindowStart());
        assertEquals(alert.getWindowEnd(), restored.getWindowEnd());
        assertEquals(alert.getStatus(), restored.getStatus());
        assertEquals(alert.getCreatedAt(), restored.getCreatedAt());
    }
}

package com.benhsoan.persistence.mapper.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.persistence.entity.security.SecurityAlertEntity;

@Component
public class SecurityAlertPersistenceMapper {

    private static final ZoneOffset UTC = ZoneOffset.UTC;

    public SecurityAlert toDomain(SecurityAlertEntity e) {
        if (e == null) {
            return null;
        }
        return SecurityAlert.restore(
                UUID.fromString(e.getId()),
                UUID.fromString(e.getUserId()),
                e.getAlertType(),
                e.getSeverity(),
                e.getDescription(),
                e.getAccessCount(),
                toInstant(e.getWindowStart()),
                toInstant(e.getWindowEnd()),
                e.getStatus(),
                toInstant(e.getCreatedAt()));
    }

    public SecurityAlertEntity toEntity(SecurityAlert d) {
        if (d == null) {
            return null;
        }
        return SecurityAlertEntity.builder()
                .id(d.getId().toString())
                .userId(d.getUserId().toString())
                .alertType(d.getAlertType())
                .severity(d.getSeverity())
                .description(d.getDescription())
                .accessCount(d.getAccessCount())
                .windowStart(toLocalDateTime(d.getWindowStart()))
                .windowEnd(toLocalDateTime(d.getWindowEnd()))
                .status(d.getStatus())
                .createdAt(toLocalDateTime(d.getCreatedAt()))
                .build();
    }

    public LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, UTC);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(UTC);
    }
}

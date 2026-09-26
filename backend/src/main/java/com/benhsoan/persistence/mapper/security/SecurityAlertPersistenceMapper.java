package com.benhsoan.persistence.mapper.security;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.persistence.entity.security.SecurityAlertEntity;

@Component
public class SecurityAlertPersistenceMapper {

    public SecurityAlert toDomain(SecurityAlertEntity e) {
        if (e == null) {
            return null;
        }
        return SecurityAlert.restore(
                e.getId(),
                e.getUserId(),
                e.getAlertType(),
                e.getSeverity(),
                e.getDescription(),
                e.getAccessCount(),
                e.getWindowStart(),
                e.getWindowEnd(),
                e.getStatus(),
                e.getCreatedAt());
    }

    public SecurityAlertEntity toEntity(SecurityAlert d) {
        if (d == null) {
            return null;
        }
        return SecurityAlertEntity.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .alertType(d.getAlertType())
                .severity(d.getSeverity())
                .description(d.getDescription())
                .accessCount(d.getAccessCount())
                .windowStart(d.getWindowStart())
                .windowEnd(d.getWindowEnd())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .build();
    }
}

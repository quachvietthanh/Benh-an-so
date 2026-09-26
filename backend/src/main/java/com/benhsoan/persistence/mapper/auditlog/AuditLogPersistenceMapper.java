package com.benhsoan.persistence.mapper.auditlog;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.persistence.entity.auditlog.AuditLogEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

@Component
public class AuditLogPersistenceMapper {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    public AuditLog toDomain(AuditLogEntity entity) {

        if (entity == null) {
            return null;
        }

        return AuditLog.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getActionType(),
                entity.getResourceType(),
                entity.getResourceId(),
                fromJsonDetail(entity.getDetail()),
                null,
                entity.getCreatedAt()
        );
    }

    public AuditLogEntity toEntity(AuditLog domain) {

        if (domain == null) {
            return null;
        }

        return AuditLogEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .actionType(domain.getActionType())
                .resourceType(domain.getResourceType())
                .resourceId(domain.getResourceId())
                .detail(toJsonDetail(domain.getDetail()))
                .ipAddress(null)
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private String toJsonDetail(String detail) {
        if (detail == null) {
            return null;
        }

        try {
            OBJECT_MAPPER.readTree(detail);
            return detail;
        } catch (JsonProcessingException ignored) {
            try {
                return OBJECT_MAPPER.writeValueAsString(detail);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Could not serialize audit log detail.", exception);
            }
        }
    }

    private String fromJsonDetail(String detail) {
        if (detail == null) {
            return null;
        }

        try {
            var node = OBJECT_MAPPER.readTree(detail);
            return node.isTextual() ? node.asText() : detail;
        } catch (JsonProcessingException ignored) {
            return detail;
        }
    }
}

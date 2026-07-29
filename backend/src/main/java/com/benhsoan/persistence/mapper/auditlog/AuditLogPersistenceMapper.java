package com.benhsoan.persistence.mapper.auditlog;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.persistence.entity.auditlog.AuditLogEntity;

@Component
public class AuditLogPersistenceMapper {

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
                entity.getDetail(),
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
                .detail(domain.getDetail())
                .ipAddress(null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

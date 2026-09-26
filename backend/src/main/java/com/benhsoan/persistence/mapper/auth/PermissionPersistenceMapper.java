package com.benhsoan.persistence.mapper.auth;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.Permission;
import com.benhsoan.persistence.entity.auth.PermissionEntity;

@Component
public class PermissionPersistenceMapper {

    public Permission toDomain(PermissionEntity entity) {
        if (entity == null) return null;
        return Permission.restore(entity.getId(), entity.getCode(), entity.getName(), entity.getModule(),
                entity.getDescription(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public PermissionEntity toEntity(Permission domain) {
        if (domain == null) return null;
        return PermissionEntity.builder()
                .id(domain.getId()).code(domain.getCode()).name(domain.getName()).module(domain.getModule())
                .description(domain.getDescription()).active(domain.isActive())
                .createdAt(domain.getCreatedAt()).updatedAt(domain.getUpdatedAt()).build();
    }
}

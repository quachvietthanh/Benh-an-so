package com.benhsoan.persistence.mapper.auth;

import java.util.HashSet;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.Role;
import com.benhsoan.persistence.entity.auth.PermissionEntity;
import com.benhsoan.persistence.entity.auth.RoleEntity;

@Component
public class RolePersistenceMapper {

    public Role toDomain(RoleEntity entity) {
        if (entity == null) {
            return null;
        }

        return Role.restore(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isSystem(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPermissions().stream()
                        .map(permission -> com.benhsoan.domain.auth.Permission.restore(
                                permission.getId(), permission.getCode(), permission.getName(), permission.getModule(),
                                permission.getDescription(), permission.isActive(), permission.getCreatedAt(), permission.getUpdatedAt()))
                        .collect(java.util.stream.Collectors.toSet())
        );
    }

    public RoleEntity toEntity(Role domain) {
        if (domain == null) {
            return null;
        }

        return RoleEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .isSystem(domain.isSystem())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .permissions(domain.getPermissions().stream()
                        .map(permission -> PermissionEntity.builder()
                                .id(permission.getId()).code(permission.getCode()).name(permission.getName())
                                .module(permission.getModule()).description(permission.getDescription())
                                .active(permission.isActive()).createdAt(permission.getCreatedAt())
                                .updatedAt(permission.getUpdatedAt()).build())
                        .collect(java.util.stream.Collectors.toSet()))
                .build();
    }
}

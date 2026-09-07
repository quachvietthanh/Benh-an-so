package com.benhsoan.persistence.mapper.auth;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.User;
import com.benhsoan.persistence.entity.auth.UserEntity;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return User.restore(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getRoleId(),
                entity.isActive(),
                entity.isMustChangePassword(),
                entity.getLastLoginAt(),
                entity.getCreatedAt()
        );
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        return UserEntity.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .passwordHash(domain.getPasswordHash())
                .fullName(domain.getFullName())
                .email(domain.getEmail())
                .phone(domain.getPhone())
                .roleId(domain.getRoleId())
                .active(domain.isActive())
                .mustChangePassword(domain.isMustChangePassword())
                .lastLoginAt(domain.getLastLoginAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
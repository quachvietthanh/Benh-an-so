package com.benhsoan.persistence.mapper.auth;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.UserSession;
import com.benhsoan.persistence.entity.auth.UserSessionEntity;

@Component
public class UserSessionPersistenceMapper {

    public UserSession toDomain(UserSessionEntity entity) {

        if (entity == null) {
            return null;
        }

        return UserSession.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getRefreshTokenHash(),
                entity.getPreviousRefreshTokenHash(),
                entity.getRefreshExpiresAt(),
                entity.getCreatedAt(),
                entity.getLastUsedAt(),
                entity.getRevokedAt()
        );
    }

    public UserSessionEntity toEntity(UserSession domain) {

        if (domain == null) {
            return null;
        }

        return UserSessionEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .refreshTokenHash(domain.getRefreshTokenHash())
                .previousRefreshTokenHash(domain.getPreviousRefreshTokenHash())
                .refreshExpiresAt(domain.getRefreshExpiresAt())
                .createdAt(domain.getCreatedAt())
                .lastUsedAt(domain.getLastUsedAt())
                .revokedAt(domain.getRevokedAt())
                .build();
    }
}

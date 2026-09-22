package com.benhsoan.persistence.mapper.auth;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.TwoFactorChallenge;
import com.benhsoan.persistence.entity.auth.TwoFactorChallengeEntity;

@Component
public class TwoFactorChallengePersistenceMapper {

    public TwoFactorChallenge toDomain(TwoFactorChallengeEntity entity) {
        if (entity == null) {
            return null;
        }
        return TwoFactorChallenge.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getCodeHash(),
                entity.getExpiresAt(),
                entity.getAttempts(),
                entity.getConsumedAt(),
                entity.getCreatedAt()
        );
    }

    public TwoFactorChallengeEntity toEntity(TwoFactorChallenge domain) {
        if (domain == null) {
            return null;
        }
        return TwoFactorChallengeEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .codeHash(domain.getCodeHash())
                .expiresAt(domain.getExpiresAt())
                .attempts(domain.getAttempts())
                .consumedAt(domain.getConsumedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

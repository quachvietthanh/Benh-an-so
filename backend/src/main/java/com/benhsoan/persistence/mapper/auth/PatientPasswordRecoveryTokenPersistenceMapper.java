package com.benhsoan.persistence.mapper.auth;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auth.PatientPasswordRecoveryToken;
import com.benhsoan.persistence.entity.auth.PatientPasswordRecoveryTokenEntity;

@Component
public class PatientPasswordRecoveryTokenPersistenceMapper {

    public PatientPasswordRecoveryToken toDomain(PatientPasswordRecoveryTokenEntity entity) {
        if (entity == null) {
            return null;
        }

        return PatientPasswordRecoveryToken.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getPhone(),
                entity.getCodeHash(),
                entity.getExpiresAt(),
                entity.getAttempts(),
                entity.getUsedAt(),
                entity.getCreatedAt()
        );
    }

    public PatientPasswordRecoveryTokenEntity toEntity(PatientPasswordRecoveryToken domain) {
        if (domain == null) {
            return null;
        }

        return PatientPasswordRecoveryTokenEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .phone(domain.getPhone())
                .codeHash(domain.getCodeHash())
                .expiresAt(domain.getExpiresAt())
                .attempts(domain.getAttempts())
                .usedAt(domain.getUsedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}

package com.benhsoan.persistence.adapterRepository.anonymization;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.anonymization.AnonymizationMode;
import com.benhsoan.persistence.entity.anonymization.SystemConfigurationEntity;
import com.benhsoan.persistence.jpaRepository.anonymization.JpaSystemConfigurationRepository;
import com.benhsoan.port.outbound.repository.anonymization.AnonymizationModeRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AnonymizationModeRepositoryAdapter implements AnonymizationModeRepository {

    public static final String ANONYMIZATION_ENABLED_KEY = "anonymization.enabled";

    private final JpaSystemConfigurationRepository jpaRepository;

    @Override
    public Optional<AnonymizationMode> find() {
        return jpaRepository.findById(ANONYMIZATION_ENABLED_KEY).map(this::toMode);
    }

    @Override
    public AnonymizationMode save(AnonymizationMode mode, UUID updatedBy) {
        SystemConfigurationEntity entity = SystemConfigurationEntity.builder()
                .configKey(ANONYMIZATION_ENABLED_KEY)
                .configValue(Boolean.toString(mode.enabled()))
                .updatedBy(updatedBy)
                .updatedAt(mode.updatedAt())
                .build();
        jpaRepository.saveAndFlush(entity);
        return mode;
    }

    private AnonymizationMode toMode(SystemConfigurationEntity entity) {
        return AnonymizationMode.of(Boolean.parseBoolean(entity.getConfigValue()), entity.getUpdatedAt());
    }
}

package com.benhsoan.persistence.jpaRepository.anonymization;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.anonymization.SystemConfigurationEntity;

public interface JpaSystemConfigurationRepository
        extends JpaRepository<SystemConfigurationEntity, String> {
}

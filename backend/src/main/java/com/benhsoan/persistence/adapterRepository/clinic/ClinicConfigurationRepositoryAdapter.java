package com.benhsoan.persistence.adapterRepository.clinic;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.persistence.jpaRepository.clinic.JpaClinicConfigurationRepository;
import com.benhsoan.persistence.mapper.clinic.ClinicConfigurationPersistenceMapper;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClinicConfigurationRepositoryAdapter implements ClinicConfigurationRepository {

    private final JpaClinicConfigurationRepository jpaRepository;
    private final ClinicConfigurationPersistenceMapper mapper;

    @Override
    public Optional<ClinicConfiguration> find() {
        return jpaRepository.findById(ClinicConfiguration.SINGLETON_ID).map(mapper::toDomain);
    }

    @Override
    public ClinicConfiguration save(ClinicConfiguration configuration) {
        Objects.requireNonNull(configuration, "Clinic configuration must not be null.");
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(configuration)));
    }
}

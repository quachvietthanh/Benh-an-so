package com.benhsoan.persistence.adapterRepository.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.persistence.jpaRepository.security.JpaSecurityAlertRepository;
import com.benhsoan.persistence.mapper.security.SecurityAlertPersistenceMapper;
import com.benhsoan.port.outbound.repository.security.SecurityAlertRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SecurityAlertRepositoryAdapter implements SecurityAlertRepository {

    private final JpaSecurityAlertRepository jpaRepository;
    private final SecurityAlertPersistenceMapper mapper;

    @Override
    public SecurityAlert save(SecurityAlert alert) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(alert)));
    }

    @Override
    public Page<SecurityAlert> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Optional<SecurityAlert> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SecurityAlert> findLatestActiveAlert(
            UUID userId,
            AlertType alertType,
            Instant createdAfter
    ) {
        return jpaRepository.findTopByUserIdAndAlertTypeAndCreatedAtAfterOrderByCreatedAtDesc(
                        userId, alertType, createdAfter)
                .map(mapper::toDomain);
    }
}

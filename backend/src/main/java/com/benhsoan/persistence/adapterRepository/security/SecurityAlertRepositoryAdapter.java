package com.benhsoan.persistence.adapterRepository.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    public List<SecurityAlert> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUserIdAndAlertTypeAndWindowStart(
            UUID userId,
            AlertType alertType,
            Instant windowStart
    ) {
        return jpaRepository.existsByUserIdAndAlertTypeAndWindowStart(
                userId.toString(),
                alertType,
                mapper.toLocalDateTime(windowStart));
    }
}

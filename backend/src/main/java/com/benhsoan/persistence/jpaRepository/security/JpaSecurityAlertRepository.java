package com.benhsoan.persistence.jpaRepository.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.persistence.entity.security.SecurityAlertEntity;

public interface JpaSecurityAlertRepository extends JpaRepository<SecurityAlertEntity, UUID> {

    Optional<SecurityAlertEntity> findByUserIdAndAlertTypeAndWindowStart(
            UUID userId,
            AlertType alertType,
            Instant windowStart
    );
}

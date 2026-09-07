package com.benhsoan.port.outbound.repository.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertType;

public interface SecurityAlertRepository {

    SecurityAlert save(SecurityAlert alert);

    Page<SecurityAlert> findAll(Pageable pageable);

    Optional<SecurityAlert> findByUserIdAndAlertTypeAndWindowStart(
            UUID userId,
            AlertType alertType,
            Instant windowStart
    );
}

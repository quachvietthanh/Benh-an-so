package com.benhsoan.port.outbound.repository.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.security.SecurityAlert;
import com.benhsoan.domain.security.enums.AlertType;

public interface SecurityAlertRepository {

    SecurityAlert save(SecurityAlert alert);

    List<SecurityAlert> findAll();

    boolean existsByUserIdAndAlertTypeAndWindowStart(
            UUID userId,
            AlertType alertType,
            Instant windowStart
    );
}

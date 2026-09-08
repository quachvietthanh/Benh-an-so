package com.benhsoan.port.inbound.security;

import java.util.UUID;

import com.benhsoan.domain.security.enums.AlertStatus;
import com.benhsoan.port.dto.result.security.SecurityAlertResult;

public interface UpdateSecurityAlertStatusUseCase {

    SecurityAlertResult updateStatus(UUID id, AlertStatus status);
}

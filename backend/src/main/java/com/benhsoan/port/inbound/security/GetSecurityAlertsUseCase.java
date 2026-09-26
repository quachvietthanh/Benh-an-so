package com.benhsoan.port.inbound.security;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.port.dto.result.security.SecurityAlertResult;

public interface GetSecurityAlertsUseCase {

    Page<SecurityAlertResult> getSecurityAlerts(Pageable pageable);
}

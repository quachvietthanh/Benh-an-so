package com.benhsoan.port.inbound.security;

import java.util.List;

import com.benhsoan.port.dto.result.security.SecurityAlertResult;

public interface GetSecurityAlertsUseCase {

    List<SecurityAlertResult> getSecurityAlerts();
}

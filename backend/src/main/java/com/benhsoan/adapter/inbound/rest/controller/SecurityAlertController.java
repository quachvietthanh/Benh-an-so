package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.SecurityAlertRestMapper;
import com.benhsoan.adapter.inbound.rest.response.security.SecurityAlertResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.security.GetSecurityAlertsUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/security-alerts")
@RequiredArgsConstructor
public class SecurityAlertController {

    private final GetSecurityAlertsUseCase getSecurityAlertsUseCase;
    private final SecurityAlertRestMapper mapper;

    @GetMapping
    @RequirePermission("SECURITY_ALERT_VIEW")
    public List<SecurityAlertResponse> getSecurityAlerts() {
        return mapper.toResponse(getSecurityAlertsUseCase.getSecurityAlerts());
    }
}

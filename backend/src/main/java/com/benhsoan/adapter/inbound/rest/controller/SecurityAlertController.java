package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.SecurityAlertRestMapper;
import com.benhsoan.adapter.inbound.rest.response.security.SecurityAlertResponse;
import com.benhsoan.domain.security.enums.AlertStatus;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.security.GetSecurityAlertsUseCase;
import com.benhsoan.port.inbound.security.UpdateSecurityAlertStatusUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/security-alerts")
@RequiredArgsConstructor
public class SecurityAlertController {

    private final GetSecurityAlertsUseCase getSecurityAlertsUseCase;
    private final UpdateSecurityAlertStatusUseCase updateSecurityAlertStatusUseCase;
    private final SecurityAlertRestMapper mapper;

    @GetMapping
    @RequirePermission("SECURITY_ALERT_VIEW")
    public Page<SecurityAlertResponse> getSecurityAlerts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return mapper.toResponse(getSecurityAlertsUseCase.getSecurityAlerts(pageable));
    }

    @PatchMapping("/{id}/status")
    @RequirePermission("SECURITY_ALERT_VIEW")
    public SecurityAlertResponse updateStatus(
            @PathVariable UUID id,
            @RequestParam AlertStatus status) {
        return mapper.toResponse(updateSecurityAlertStatusUseCase.updateStatus(id, status));
    }
}

package com.benhsoan.adapter.inbound.rest.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public Page<SecurityAlertResponse> getSecurityAlerts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return mapper.toResponse(getSecurityAlertsUseCase.getSecurityAlerts(pageable));
    }
}

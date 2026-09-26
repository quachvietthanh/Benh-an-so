package com.benhsoan.adapter.inbound.rest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.port.dto.result.OperationalDashboardResult;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.dashboard.GetOperationalDashboardUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class OperationalDashboardController {

    private final GetOperationalDashboardUseCase getOperationalDashboardUseCase;

    @GetMapping("/operational")
    @RequirePermission("DASHBOARD_OPERATIONAL_READ")
    public OperationalDashboardResult getOperationalDashboard() {
        return getOperationalDashboardUseCase.get();
    }
}

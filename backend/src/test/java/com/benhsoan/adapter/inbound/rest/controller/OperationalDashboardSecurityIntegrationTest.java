package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.config.SecurityConfig;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.OperationalDashboardResult;
import com.benhsoan.port.inbound.dashboard.GetOperationalDashboardUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = OperationalDashboardController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class,
        RequirePermissionAspect.class, PermissionEvaluator.class, OperationalDashboardSecurityIntegrationTest.AspectTestConfig.class})
class OperationalDashboardSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig { }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetOperationalDashboardUseCase getOperationalDashboardUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void onlyAdminsAndManagersCanReadOperationalDashboard() throws Exception {
        when(getOperationalDashboardUseCase.get())
                .thenReturn(new OperationalDashboardResult(
                        new OperationalDashboardResult.VisitSummary(0, 0, 0, 0, 0),
                        new OperationalDashboardResult.RevenueSummary(BigDecimal.ZERO),
                        new OperationalDashboardResult.InventoryAlertSummary(0, 0),
                        Instant.parse("2026-08-11T08:00:00Z")
                ));

        mockMvc.perform(get("/dashboard/operational").with(user("admin").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_DASHBOARD_OPERATIONAL_READ"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dashboard/operational").with(user("manager").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_DASHBOARD_OPERATIONAL_READ"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dashboard/operational").with(user("manager").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_DASHBOARD_OPERATIONAL_READ"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard/operational").with(user("doctor").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_CARE_LOG_READ"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/dashboard/operational").with(user("receptionist").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("PERMISSION_CARE_LOG_READ"))))
                .andExpect(status().isForbidden());
    }
}

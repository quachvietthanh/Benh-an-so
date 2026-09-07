package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.SecurityAlertRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.security.enums.AlertSeverity;
import com.benhsoan.domain.security.enums.AlertStatus;
import com.benhsoan.domain.security.enums.AlertType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.security.SecurityAlertResult;
import com.benhsoan.port.inbound.security.GetSecurityAlertsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = SecurityAlertController.class)
@Import({
        AopAutoConfiguration.class,
        SecurityAlertSecurityIntegrationTest.AspectTestConfig.class,
        SecurityAlertRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class SecurityAlertSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSecurityAlertsUseCase getSecurityAlertsUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private ClockPort clockPort;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @Test
    void forbidsUserWithoutSecurityAlertViewPermission() throws Exception {
        mockMvc.perform(get("/security-alerts")
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getSecurityAlertsUseCase);
    }

    @Test
    void allowsAdminWithSecurityAlertViewPermissionAndReturnsAlerts() throws Exception {
        UUID userId = UUID.randomUUID();
        when(getSecurityAlertsUseCase.getSecurityAlerts(any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(
                        new SecurityAlertResult(
                                UUID.randomUUID(),
                                userId,
                                "admin",
                                "System Administrator",
                                AlertType.THRESHOLD_EXCEEDED,
                                AlertSeverity.HIGH,
                                "Threshold exceeded",
                                25,
                                Instant.parse("2026-08-11T10:00:00Z"),
                                Instant.parse("2026-08-11T11:00:00Z"),
                                AlertStatus.UNREAD,
                                Instant.parse("2026-08-11T10:05:00Z")
                        )
                ),
                PageRequest.of(0, 20),
                1
        ));

        mockMvc.perform(get("/security-alerts")
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("PERMISSION_SECURITY_ALERT_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].alertType").value("THRESHOLD_EXCEEDED"))
                .andExpect(jsonPath("$.content[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.content[0].accessCount").value(25))
                .andExpect(jsonPath("$.content[0].username").value("admin"))
                .andExpect(jsonPath("$.content[0].fullName").value("System Administrator"));
    }
}

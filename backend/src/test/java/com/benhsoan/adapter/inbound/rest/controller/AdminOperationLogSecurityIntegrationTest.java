package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.AdminOperationLogRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.inbound.auditlog.GetAdminOperationLogsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = AdminOperationLogController.class)
@Import({
        AopAutoConfiguration.class,
        AdminOperationLogSecurityIntegrationTest.AspectTestConfig.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        AdminOperationLogRestMapper.class
})
class AdminOperationLogSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GetAdminOperationLogsUseCase getAdminOperationLogsUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void adminWithPermissionCanReadLogs() throws Exception {
        when(getAdminOperationLogsUseCase.getLogs(any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/admin-operation-logs")
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("PERMISSION_ADMIN_OPERATION_LOG_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    void managerWithPermissionCanReadLogs() throws Exception {
        when(getAdminOperationLogsUseCase.getLogs(any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/admin-operation-logs")
                        .with(user("manager").authorities(
                                new SimpleGrantedAuthority("PERMISSION_ADMIN_OPERATION_LOG_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    void doctorWithoutPermissionIsDenied() throws Exception {
        mockMvc.perform(get("/admin-operation-logs")
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deniedAccessIsLoggedAsAccessDeniedWithoutRecursion() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        mockMvc.perform(get("/admin-operation-logs")
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ"))))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(ActionType.ACCESS_DENIED, captor.getValue().getActionType());
        assertEquals(ResourceType.PERMISSION, captor.getValue().getResourceType());
    }

    @Test
    void rejectsInvertedDateRange() throws Exception {
        mockMvc.perform(get("/admin-operation-logs")
                        .param("from", "2026-09-30T00:00:00Z")
                        .param("to", "2026-09-01T00:00:00Z")
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("PERMISSION_ADMIN_OPERATION_LOG_READ"))))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("from must be before or equal to to."));
    }
}

package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import com.benhsoan.port.dto.result.auditlog.AdminOperationLogResult;
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

    @Test
    void serializesNonEmptyAdminOperationLogResponse() throws Exception {
        UUID actorId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID resourceId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        String detail = "{\"before\":{\"price\":95000.00,\"effectiveFrom\":\"2026-01-01\"},\"after\":{\"price\":120000.00,\"effectiveFrom\":\"2026-09-01\"}}";
        AdminOperationLogResult result = new AdminOperationLogResult(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                actorId,
                "System Administrator",
                ActionType.CREATE,
                ResourceType.SERVICE_PRICE,
                resourceId,
                detail,
                Instant.parse("2026-09-17T03:30:00Z"));
        when(getAdminOperationLogsUseCase.getLogs(any(), any()))
                .thenReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/admin-operation-logs")
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("PERMISSION_ADMIN_OPERATION_LOG_READ"))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].id")
                        .value("11111111-1111-1111-1111-111111111111"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].actorId")
                        .value("22222222-2222-2222-2222-222222222222"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].actorName")
                        .value("System Administrator"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].actionType")
                        .value("CREATE"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].resourceType")
                        .value("SERVICE_PRICE"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].resourceId")
                        .value("33333333-3333-3333-3333-333333333333"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].detail")
                        .value(detail))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].createdAt")
                        .value("2026-09-17T03:30:00Z"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalElements")
                        .value(1));
    }
}

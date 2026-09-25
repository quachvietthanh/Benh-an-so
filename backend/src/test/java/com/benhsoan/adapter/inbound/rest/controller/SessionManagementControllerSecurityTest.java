package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
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

import com.benhsoan.adapter.inbound.rest.mapper.SessionRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.inbound.auth.GetActiveSessionsUseCase;
import com.benhsoan.port.inbound.auth.TerminateSessionUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = SessionManagementController.class)
@Import({
        SessionRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        AopAutoConfiguration.class,
        SessionManagementControllerSecurityTest.AspectTestConfig.class
})
@DisplayName("SessionManagementController Security Integration Tests (AC-03, AC-04, QTN-01)")
class SessionManagementControllerSecurityTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GetActiveSessionsUseCase getActiveSessionsUseCase;
    @MockitoBean private TerminateSessionUseCase terminateSessionUseCase;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Request chưa xác thực bị từ chối 401 Unauthorized")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/admin/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("AC-04 / QTN-01: Bác sĩ mở danh sách phiên người khác bị từ chối 403 Forbidden và được ghi nhật ký kiểm toán")
    void doctorWithoutSessionReadIsForbiddenAndDenialAudited() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        mockMvc.perform(get("/admin/sessions")
                        .with(user("doctor1").authorities(
                                new SimpleGrantedAuthority("ROLE_DOCTOR"),
                                new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ")
                        )))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(ActionType.ACCESS_DENIED, captor.getValue().getActionType());
    }

    @Test
    @DisplayName("AC-03: Quản trị viên có quyền SESSION_READ xem được danh sách phiên (200 OK)")
    void adminWithSessionReadIsAllowed() throws Exception {
        when(getActiveSessionsUseCase.getActiveSessions(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/admin/sessions")
                        .with(user("admin1").authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("PERMISSION_SESSION_READ")
                        )))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AC-03: Quản trị viên có quyền SESSION_TERMINATE kết thúc phiên từ xa thành công (204 No Content)")
    void adminWithSessionTerminateCanTerminateSession() throws Exception {
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(post("/admin/sessions/{id}/terminate", sessionId)
                        .with(user("admin1").authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("PERMISSION_SESSION_TERMINATE")
                        )))
                .andExpect(status().isNoContent());

        verify(terminateSessionUseCase).terminateSession(any());
    }
}

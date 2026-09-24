package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.benhsoan.adapter.inbound.rest.mapper.SessionRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.inbound.session.ExtendSessionUseCase;
import com.benhsoan.port.inbound.session.GetCurrentSessionUseCase;
import com.benhsoan.port.inbound.session.ListSessionsUseCase;
import com.benhsoan.port.inbound.session.TerminateSessionUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = SessionController.class)
@Import({
        AopAutoConfiguration.class,
        SessionSecurityIntegrationTest.AspectTestConfig.class,
        SessionRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class SessionSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetCurrentSessionUseCase getCurrentSessionUseCase;
    @MockitoBean
    private ExtendSessionUseCase extendSessionUseCase;
    @MockitoBean
    private ListSessionsUseCase listSessionsUseCase;
    @MockitoBean
    private TerminateSessionUseCase terminateSessionUseCase;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserSessionRepository userSessionRepository;
    @MockitoBean
    private RoleRepository roleRepository;
    @MockitoBean
    private AuditLogRepository auditLogRepository;
    @MockitoBean
    private CurrentUserPort currentUserPort;
    @MockitoBean
    private ClockPort clockPort;

    @Test
    void forbidsUserWithoutSessionReadPermission() throws Exception {
        mockMvc.perform(get("/sessions")
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("PERMISSION_MEDICAL_RECORD_READ"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(listSessionsUseCase);
    }

    @Test
    void allowsAdminWithSessionReadPermission() throws Exception {
        when(listSessionsUseCase.list(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/sessions")
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("PERMISSION_SESSION_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    void forbidsUserWithoutSessionTerminatePermission() throws Exception {
        mockMvc.perform(delete("/sessions/" + UUID.randomUUID())
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("PERMISSION_SESSION_READ"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(terminateSessionUseCase);
    }

    @Test
    void allowsAdminWithSessionTerminatePermission() throws Exception {
        mockMvc.perform(delete("/sessions/" + UUID.randomUUID())
                        .with(user("admin").authorities(
                                new SimpleGrantedAuthority("PERMISSION_SESSION_TERMINATE"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void requiresAuthenticationForCurrentSession() throws Exception {
        mockMvc.perform(get("/sessions/current"))
                .andExpect(status().isUnauthorized());
    }
}
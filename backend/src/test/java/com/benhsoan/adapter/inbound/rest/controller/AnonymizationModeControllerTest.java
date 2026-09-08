package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.AnonymizationModeRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.command.anonymization.UpdateAnonymizationModeCommand;
import com.benhsoan.port.dto.result.anonymization.AnonymizationModeResult;
import com.benhsoan.port.inbound.anonymization.GetAnonymizationModeUseCase;
import com.benhsoan.port.inbound.anonymization.UpdateAnonymizationModeUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = AnonymizationModeController.class)
@Import({
        AopAutoConfiguration.class,
        AnonymizationModeControllerTest.AspectTestConfig.class,
        AnonymizationModeRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class
})
class AnonymizationModeControllerTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-09-08T14:30:00Z");

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAnonymizationModeUseCase getAnonymizationModeUseCase;
    @MockitoBean
    private UpdateAnonymizationModeUseCase updateAnonymizationModeUseCase;
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
    void adminReadsMode() throws Exception {
        when(getAnonymizationModeUseCase.get()).thenReturn(new AnonymizationModeResult(false, UPDATED_AT));

        mockMvc.perform(get("/system/anonymization")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_SYSTEM_CONFIG_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(getAnonymizationModeUseCase).get();
    }

    @Test
    void adminUpdatesMode() throws Exception {
        when(updateAnonymizationModeUseCase.update(any()))
                .thenReturn(new AnonymizationModeResult(true, UPDATED_AT));

        mockMvc.perform(patch("/system/anonymization")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_SYSTEM_CONFIG_UPDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(updateAnonymizationModeUseCase).update(any(UpdateAnonymizationModeCommand.class));
    }

    @Test
    void rejectsNonAdminUpdate() throws Exception {
        mockMvc.perform(patch("/system/anonymization")
                        .with(user("doctor").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(updateAnonymizationModeUseCase, getAnonymizationModeUseCase);
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/system/anonymization"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(getAnonymizationModeUseCase, updateAnonymizationModeUseCase);
    }

    @Test
    void rejectsMissingEnabledField() throws Exception {
        mockMvc.perform(patch("/system/anonymization")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_SYSTEM_CONFIG_UPDATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(updateAnonymizationModeUseCase);
    }
}

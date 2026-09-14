package com.benhsoan.adapter.inbound.rest.controller;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.DiagnosisCatalogRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;
import com.benhsoan.port.dto.result.DiagnosisSuggestionResult;
import com.benhsoan.port.inbound.medicalrecord.GetDiagnosisCatalogUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetDiagnosisSuggestionsUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = DiagnosisCatalogController.class)
@Import({
        DiagnosisCatalogRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        AopAutoConfiguration.class,
        DiagnosisCatalogSuggestionsSecurityIntegrationTest.AspectTestConfig.class
})
class DiagnosisCatalogSuggestionsSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetDiagnosisCatalogUseCase getDiagnosisCatalogUseCase;
    @MockitoBean
    private GetDiagnosisSuggestionsUseCase getDiagnosisSuggestionsUseCase;
    @MockitoBean
    private CurrentUserPort currentUserPort;
    @MockitoBean
    private JwtTokenPort jwtTokenPort;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserSessionRepository userSessionRepository;
    @MockitoBean
    private ClockPort clockPort;
    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/diagnosis-catalog/suggestions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedDoctorWithoutDiagnosisReadIsForbidden() throws Exception {
        mockMvc.perform(get("/diagnosis-catalog/suggestions")
                        .with(user("doctor1").authorities(new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void doctorWithDiagnosisReadReceivesSuggestions() throws Exception {
        var recent = new DiagnosisCatalogResult(
                UUID.randomUUID(), "J02.9", "Viêm họng cấp", null, "Hệ hô hấp", null, true, Instant.now(), null);
        when(getDiagnosisSuggestionsUseCase.suggest())
                .thenReturn(new DiagnosisSuggestionResult(List.of(recent), List.of(), List.of("Hệ hô hấp")));

        mockMvc.perform(get("/diagnosis-catalog/suggestions")
                        .with(user("doctor1").authorities(new SimpleGrantedAuthority("PERMISSION_DIAGNOSIS_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recent[0].code").value("J02.9"))
                .andExpect(jsonPath("$.diseaseGroups[0]").value("Hệ hô hấp"));
    }
}

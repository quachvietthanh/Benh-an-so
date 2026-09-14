package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalServiceCatalogManagementRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.inbound.clinical.CreateClinicalReferenceRangeUseCase;
import com.benhsoan.port.inbound.clinical.CreateClinicalServiceUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalReferenceRangesUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalServicesUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalReferenceRangeStatusUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalReferenceRangeUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalServiceStatusUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalServiceUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ClinicalServiceCatalogManagementController.class)
@Import({
        ClinicalServiceCatalogManagementRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        AopAutoConfiguration.class,
        ClinicalServiceCatalogManagementSecurityIntegrationTest.AspectTestConfig.class
})
class ClinicalServiceCatalogManagementSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GetClinicalServicesUseCase getClinicalServicesUseCase;
    @MockitoBean private CreateClinicalServiceUseCase createClinicalServiceUseCase;
    @MockitoBean private UpdateClinicalServiceUseCase updateClinicalServiceUseCase;
    @MockitoBean private UpdateClinicalServiceStatusUseCase updateClinicalServiceStatusUseCase;
    @MockitoBean private GetClinicalReferenceRangesUseCase getClinicalReferenceRangesUseCase;
    @MockitoBean private CreateClinicalReferenceRangeUseCase createClinicalReferenceRangeUseCase;
    @MockitoBean private UpdateClinicalReferenceRangeUseCase updateClinicalReferenceRangeUseCase;
    @MockitoBean private UpdateClinicalReferenceRangeStatusUseCase updateClinicalReferenceRangeStatusUseCase;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/system/clinical-services"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pharmacistWithoutManagePermissionIsForbiddenAndDenialAudited() throws Exception {
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        mockMvc.perform(get("/system/clinical-services")
                        .with(user("pharmacist1").authorities(new SimpleGrantedAuthority("ROLE_PHARMACIST"))))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(ActionType.ACCESS_DENIED, captor.getValue().getActionType());
    }

    @Test
    void adminWithManagePermissionIsAllowed() throws Exception {
        when(getClinicalServicesUseCase.search(any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/system/clinical-services")
                        .with(user("admin1").authorities(new SimpleGrantedAuthority("PERMISSION_CLINICAL_SERVICE_MANAGE"))))
                .andExpect(status().isOk());
    }
}

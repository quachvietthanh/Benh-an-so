package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalServiceCatalogManagementRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.clinical.exception.ClinicalReferenceRangeOverlapException;
import com.benhsoan.domain.clinical.exception.ClinicalServiceCodeAlreadyExistsException;
import com.benhsoan.domain.shared.exception.ValidationException;
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
        ClinicalServiceCatalogManagementControllerTest.AspectTestConfig.class
})
class ClinicalServiceCatalogManagementControllerTest {

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
    void invalidThresholdIsRejectedWithValidationFailed() throws Exception {
        when(createClinicalReferenceRangeUseCase.create(any(UUID.class), any()))
                .thenThrow(new ValidationException("Lower bound must not exceed upper bound."));

        mockMvc.perform(post("/system/clinical-services/{serviceId}/reference-ranges", UUID.randomUUID())
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_CLINICAL_SERVICE_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"MALE\",\"minAge\":18,\"maxAge\":64,\"lowerBound\":10,\"upperBound\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void duplicateServiceCodeIsRejectedWithConflict() throws Exception {
        when(createClinicalServiceUseCase.create(any()))
                .thenThrow(new ClinicalServiceCodeAlreadyExistsException("LAB-GLU"));

        mockMvc.perform(post("/system/clinical-services")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_CLINICAL_SERVICE_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLINICAL_SERVICE_CODE_ALREADY_EXISTS"));
    }

    @Test
    void overlappingReferenceRangeIsRejectedWithConflict() throws Exception {
        when(createClinicalReferenceRangeUseCase.create(any(UUID.class), any()))
                .thenThrow(new ClinicalReferenceRangeOverlapException(
                        "Reference range overlaps an existing active range for the same gender and age group."));

        mockMvc.perform(post("/system/clinical-services/{serviceId}/reference-ranges", UUID.randomUUID())
                        .with(user("admin").authorities(new SimpleGrantedAuthority("PERMISSION_CLINICAL_SERVICE_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"MALE\",\"minAge\":18,\"maxAge\":64,\"lowerBound\":5,\"upperBound\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLINICAL_REFERENCE_RANGE_OVERLAP"));
    }

    private static String serviceBody() {
        return """
                {
                  "serviceCatalogId": "%s",
                  "serviceCode": "LAB-GLU",
                  "serviceName": "Blood glucose",
                  "serviceType": "LAB_TEST",
                  "resultDataType": "NUMBER",
                  "unit": "mmol/L",
                  "referenceRange": "3.9-5.5"
                }
                """.formatted(UUID.randomUUID());
    }
}


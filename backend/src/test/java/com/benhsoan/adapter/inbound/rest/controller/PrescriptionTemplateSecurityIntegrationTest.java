package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PrescriptionTemplateRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.PrescriptionTemplateResult;
import com.benhsoan.port.inbound.prescription.ApplyPrescriptionTemplateUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionTemplateUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionTemplatesUseCase;
import com.benhsoan.port.inbound.prescription.SavePrescriptionTemplateUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

/**
 * NCL-05-CN-008: verifies that the @RequirePermission aspect is actually enforced
 * for the prescription-template endpoints (read requires PRESCRIPTION_READ, write
 * requires PRESCRIPTION_CREATE). Service-level doctor/ownership checks are covered
 * by the corresponding service unit tests.
 */
@WebMvcTest(controllers = PrescriptionTemplateController.class)
@Import({
        PrescriptionTemplateRestMapper.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        PrescriptionTemplateSecurityIntegrationTest.AspectTestConfig.class
})
class PrescriptionTemplateSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private SavePrescriptionTemplateUseCase savePrescriptionTemplateUseCase;
    @MockitoBean private ApplyPrescriptionTemplateUseCase applyPrescriptionTemplateUseCase;
    @MockitoBean private GetPrescriptionTemplatesUseCase getPrescriptionTemplatesUseCase;
    @MockitoBean private GetPrescriptionTemplateUseCase getPrescriptionTemplateUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void getTemplatesWithoutReadPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/prescription-templates").param("diagnosisCode", "J06.9")
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTemplatesWithReadPermissionSucceeds() throws Exception {
        when(getPrescriptionTemplatesUseCase.getByDiagnosisCode("J06.9"))
                .thenReturn(List.of(templateResult()));

        mockMvc.perform(get("/prescription-templates").param("diagnosisCode", "J06.9")
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("ROLE_DOCTOR"),
                                new SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_READ"))))
                .andExpect(status().isOk());
    }

    @Test
    void getByIdWithoutReadPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/prescription-templates/{id}", UUID.randomUUID())
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("ROLE_DOCTOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveWithoutCreatePermissionIsForbidden() throws Exception {
        mockMvc.perform(post("/prescription-templates")
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("ROLE_DOCTOR"),
                                new SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prescriptionId\":\"" + UUID.randomUUID() + "\",\"diagnosisCode\":\"J06.9\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveWithCreatePermissionSucceeds() throws Exception {
        when(savePrescriptionTemplateUseCase.save(any())).thenReturn(templateResult());

        mockMvc.perform(post("/prescription-templates")
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("ROLE_DOCTOR"),
                                new SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_CREATE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prescriptionId\":\"" + UUID.randomUUID() + "\",\"diagnosisCode\":\"J06.9\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void applyWithoutCreatePermissionIsForbidden() throws Exception {
        mockMvc.perform(post("/prescription-templates/{id}/apply", UUID.randomUUID())
                        .with(user("doctor").authorities(
                                new SimpleGrantedAuthority("ROLE_DOCTOR"),
                                new SimpleGrantedAuthority("PERMISSION_PRESCRIPTION_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicalRecordId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    private PrescriptionTemplateResult templateResult() {
        UUID medicineId = UUID.randomUUID();
        return new PrescriptionTemplateResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "J06.9",
                "Viêm họng",
                UUID.randomUUID(),
                Instant.parse("2026-09-30T01:00:00Z"),
                List.of(new PrescriptionTemplateResult.Item(
                        UUID.randomUUID(), medicineId, "MED001", "Paracetamol", "Paracetamol",
                        "500mg", "viên", "1 viên", 2, AdministrationRoute.ORAL, 7, 14, null, 0))
        );
    }
}

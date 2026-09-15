package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import com.benhsoan.adapter.inbound.rest.mapper.PatientFamilyHistoryRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;
import com.benhsoan.port.inbound.patient.AddPatientFamilyHistoryUseCase;
import com.benhsoan.port.inbound.patient.DeletePatientFamilyHistoryUseCase;
import com.benhsoan.port.inbound.patient.GetPatientFamilyHistoryUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientFamilyHistoryController.class)
@Import({
        PatientFamilyHistoryRestMapper.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        GlobalExceptionHandler.class,
        PatientFamilyHistorySecurityIntegrationTest.AspectTestConfig.class
})
class PatientFamilyHistorySecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AddPatientFamilyHistoryUseCase addPatientFamilyHistoryUseCase;
    @MockitoBean private GetPatientFamilyHistoryUseCase getPatientFamilyHistoryUseCase;
    @MockitoBean private DeletePatientFamilyHistoryUseCase deletePatientFamilyHistoryUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void doctorWithWritePermissionCanAdd() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        PatientFamilyHistoryResult result = PatientFamilyHistoryResult.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .relationship("Bố")
                .diagnosisCatalogId(catalogId)
                .active(true)
                .createdAt(Instant.parse("2026-09-10T10:00:00Z"))
                .updatedAt(Instant.parse("2026-09-10T10:00:00Z"))
                .build();
        when(addPatientFamilyHistoryUseCase.addFamilyHistory(any())).thenReturn(result);

        mockMvc.perform(post("/patients/{patientId}/family-history", patientId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_FAMILY_HISTORY_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relationship\":\"Bố\",\"diagnosisCatalogId\":\"" + catalogId + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void userWithoutWritePermissionIsForbidden() throws Exception {
        mockMvc.perform(post("/patients/{patientId}/family-history", UUID.randomUUID())
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relationship\":\"Bố\",\"diagnosisCatalogId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userWithoutReadPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/patients/{patientId}/family-history", UUID.randomUUID())
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void pharmacistWithReadPermissionCanList() throws Exception {
        when(getPatientFamilyHistoryUseCase.getFamilyHistory(any())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/patients/{patientId}/family-history", UUID.randomUUID())
                        .with(user("pharmacist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_FAMILY_HISTORY_READ"))))
                .andExpect(status().isOk());
    }
}

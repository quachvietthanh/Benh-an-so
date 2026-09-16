package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientChronicDiseaseRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.patient.PatientChronicDiseaseResult;
import com.benhsoan.port.inbound.patient.AddPatientChronicDiseaseUseCase;
import com.benhsoan.port.inbound.patient.DeletePatientChronicDiseaseUseCase;
import com.benhsoan.port.inbound.patient.GetPatientChronicDiseasesUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientChronicDiseaseController.class)
@Import({
        PatientChronicDiseaseRestMapper.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RequirePermissionAspect.class,
        PermissionEvaluator.class,
        GlobalExceptionHandler.class,
        PatientChronicDiseaseSecurityIntegrationTest.AspectTestConfig.class
})
class PatientChronicDiseaseSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AddPatientChronicDiseaseUseCase addPatientChronicDiseaseUseCase;
    @MockitoBean private GetPatientChronicDiseasesUseCase getPatientChronicDiseasesUseCase;
    @MockitoBean private DeletePatientChronicDiseaseUseCase deletePatientChronicDiseaseUseCase;

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
        PatientChronicDiseaseResult result = PatientChronicDiseaseResult.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .diagnosisCatalogId(catalogId)
                .active(true)
                .createdAt(Instant.parse("2026-09-10T10:00:00Z"))
                .updatedAt(Instant.parse("2026-09-10T10:00:00Z"))
                .build();
        when(addPatientChronicDiseaseUseCase.addChronicDisease(any())).thenReturn(result);

        mockMvc.perform(post("/patients/{patientId}/chronic-diseases", patientId)
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CHRONIC_DISEASE_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosisCatalogId\":\"" + catalogId + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void userWithoutWritePermissionIsForbidden() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);

        mockMvc.perform(post("/patients/{patientId}/chronic-diseases", UUID.randomUUID())
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosisCatalogId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(currentUserId, captor.getValue().getUserId());
        assertEquals(ActionType.ACCESS_DENIED, captor.getValue().getActionType());
        assertEquals(ResourceType.PERMISSION, captor.getValue().getResourceType());
    }

    @Test
    void userWithoutReadPermissionIsForbidden() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);

        mockMvc.perform(get("/patients/{patientId}/chronic-diseases", UUID.randomUUID())
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(currentUserId, captor.getValue().getUserId());
        assertEquals(ActionType.ACCESS_DENIED, captor.getValue().getActionType());
        assertEquals(ResourceType.PERMISSION, captor.getValue().getResourceType());
    }

    @Test
    void doctorWithReadPermissionCanList() throws Exception {
        when(getPatientChronicDiseasesUseCase.getChronicDiseases(any())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/patients/{patientId}/chronic-diseases", UUID.randomUUID())
                        .with(user("doctor").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CHRONIC_DISEASE_READ"))))
                .andExpect(status().isOk());
    }
}

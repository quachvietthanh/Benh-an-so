package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.benhsoan.adapter.inbound.rest.mapper.PatientRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.infrastructure.security.annotation.RequirePermissionAspect;
import com.benhsoan.infrastructure.security.service.PermissionEvaluator;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.GetPatientByCodeUseCase;
import com.benhsoan.port.inbound.patient.GetPatientByIdUseCase;
import com.benhsoan.port.inbound.patient.RegisterPatientUseCase;
import com.benhsoan.port.inbound.patient.SearchPatientUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientController.class)
@Import({PatientRestMapper.class, SecurityConfig.class, JwtAuthenticationFilter.class,
        RequirePermissionAspect.class, PermissionEvaluator.class, PatientSecurityIntegrationTest.AspectTestConfig.class})
class PatientSecurityIntegrationTest {

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class AspectTestConfig {
    }

    @Autowired private MockMvc mockMvc;

    @MockitoBean private RegisterPatientUseCase registerPatientUseCase;
    @MockitoBean private SearchPatientUseCase searchPatientUseCase;
    @MockitoBean private UpdatePatientUseCase updatePatientUseCase;
    @MockitoBean private GetPatientByIdUseCase getPatientByIdUseCase;
    @MockitoBean private GetPatientByCodeUseCase getPatientByCodeUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void allowsManagerToReadPatientsOnly() throws Exception {
        PatientResult patient = new PatientResult(
                UUID.randomUUID(),
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1995, 5, 10),
                Gender.MALE,
                "0909000001",
                null,
                "HCM",
                null,
                null,
                null,
                null,
                null,
                true,
                Instant.parse("2026-08-12T02:00:00Z"),
                Instant.parse("2026-08-12T02:00:00Z"),
                true,
                Instant.parse("2026-08-12T02:00:00Z"),
                "v1.0",
                false,
                null,
                null,
                false
        );

        when(searchPatientUseCase.search(any())).thenReturn(new PageImpl<>(
                List.of(patient),
                PageRequest.of(0, 20),
                1
        ));
        when(getPatientByIdUseCase.getById(any())).thenReturn(patient);
        when(getPatientByCodeUseCase.getByCode(any())).thenReturn(patient);

        mockMvc.perform(get("/patients").with(user("manager")
                        .authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/patients/{patientId}", patient.id()).with(user("manager")
                        .authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/patients/code/{code}", patient.patientCode()).with(user("manager")
                        .authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/patients").with(user("pharmacist").roles("PHARMACIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsCreateAndUpdateOnlyWithTheirPermissions() throws Exception {
        PatientResult patient = patient();
        when(registerPatientUseCase.register(any())).thenReturn(patient);
        when(updatePatientUseCase.update(any(), any())).thenReturn(patient);

        mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON).content(patientRequest())
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_CREATE"))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/patients/{id}", patient.id()).contentType(MediaType.APPLICATION_JSON).content(patientRequest())
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_UPDATE"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON).content(patientRequest())
                        .with(user("receptionist").authorities(new SimpleGrantedAuthority("PERMISSION_PATIENT_READ"))))
                .andExpect(status().isForbidden());
    }

    private static String patientRequest() {
        return """
                {"fullName":"Nguyen Van A","dateOfBirth":"1995-05-10","gender":"MALE","phone":"0909000001","active":true,"consentAgreed":true,"consentVersion":"v1.0"}
                """;
    }

    private static PatientResult patient() {
        return new PatientResult(UUID.randomUUID(), "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10),
                Gender.MALE, "0909000001", null, "HCM", null, null, null, null, null, true,
                Instant.parse("2026-08-12T02:00:00Z"), Instant.parse("2026-08-12T02:00:00Z"),
                true, Instant.parse("2026-08-12T02:00:00Z"), "v1.0", false, null, null, false);
    }
}

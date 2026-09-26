package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientLinkedProfileRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.result.patient.LinkedPatientProfileResult;
import com.benhsoan.port.inbound.patient.GetPatientLinkedProfilesUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

/**
 * NCL-14-CN-010 CV-02 / CV-04: {@code GET /patient-portal/patients/linked} contract and
 * transport-level security (401 unauthenticated, 403 for a non-PATIENT role) plus the
 * proof that no clinical or direct-identifier field is exposed.
 */
@WebMvcTest(controllers = PatientPortalPatientController.class)
@Import({
        PatientLinkedProfileRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class PatientPortalPatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GetPatientLinkedProfilesUseCase getPatientLinkedProfilesUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void returnsOwnProfileFirstThenLinkedDependents() throws Exception {
        UUID ownId = UUID.randomUUID();
        UUID dependentId = UUID.randomUUID();

        when(getPatientLinkedProfilesUseCase.getLinkedProfiles()).thenReturn(List.of(
                new LinkedPatientProfileResult(ownId, "BN000001", "Nguyen Van A",
                        LocalDate.of(1995, 5, 10), 31, false, "SELF", true, false),
                new LinkedPatientProfileResult(dependentId, "BN000002", "Nguyen Van Con",
                        LocalDate.of(2015, 5, 10), 11, true, "Bo", false, false)));

        mockMvc.perform(get("/patient-portal/patients/linked")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(ownId.toString()))
                .andExpect(jsonPath("$[0].self").value(true))
                .andExpect(jsonPath("$[0].relationship").value("SELF"))
                .andExpect(jsonPath("$[0].isMinor").value(false))
                .andExpect(jsonPath("$[1].patientId").value(dependentId.toString()))
                .andExpect(jsonPath("$[1].self").value(false))
                .andExpect(jsonPath("$[1].isMinor").value(true))
                .andExpect(jsonPath("$[1].relationship").value("Bo"));
    }

    @Test
    void exposesOnlyMinimalNonClinicalFields() throws Exception {
        UUID ownId = UUID.randomUUID();

        when(getPatientLinkedProfilesUseCase.getLinkedProfiles()).thenReturn(List.of(
                new LinkedPatientProfileResult(ownId, "BN000001", "Nguyen Van A",
                        LocalDate.of(1995, 5, 10), 31, false, "SELF", true, false)));

        mockMvc.perform(get("/patient-portal/patients/linked")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].address").doesNotExist())
                .andExpect(jsonPath("$[0].identityNumber").doesNotExist())
                .andExpect(jsonPath("$[0].insuranceNumber").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].allergies").doesNotExist())
                .andExpect(jsonPath("$[0].chronicDiseases").doesNotExist())
                .andExpect(jsonPath("$[0].medicalHistory").doesNotExist())
                .andExpect(jsonPath("$[0].guardianUserId").doesNotExist());
    }

    @Test
    void exposesRequiresGuardianLinkReviewFlagForTc03() throws Exception {
        UUID dependentId = UUID.randomUUID();

        when(getPatientLinkedProfilesUseCase.getLinkedProfiles()).thenReturn(List.of(
                new LinkedPatientProfileResult(dependentId, "BN000003", "Nguyen Van Con",
                        LocalDate.of(2006, 5, 10), 19, false, "Bo", false, true)));

        mockMvc.perform(get("/patient-portal/patients/linked")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requiresGuardianLinkReview").value(true));
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/patient-portal/patients/linked"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns403ForNonPatientRole() throws Exception {
        mockMvc.perform(get("/patient-portal/patients/linked")
                        .with(user("staff").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }
}

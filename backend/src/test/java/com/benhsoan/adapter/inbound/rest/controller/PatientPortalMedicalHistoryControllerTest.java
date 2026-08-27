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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalMedicalHistoryRestMapper;
import com.benhsoan.config.SecurityConfig;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.infrastructure.authSecurity.JwtAuthenticationFilter;
import com.benhsoan.port.dto.result.patient.PatientMedicalHistoryDetailResult;
import com.benhsoan.port.dto.result.patient.PatientMedicalHistorySummaryResult;
import com.benhsoan.port.inbound.patient.GetPatientMedicalHistoryDetailUseCase;
import com.benhsoan.port.inbound.patient.GetPatientMedicalHistoryUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.RoleRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientPortalMedicalHistoryController.class)
@Import({
        PatientPortalMedicalHistoryRestMapper.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
class PatientPortalMedicalHistoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private GetPatientMedicalHistoryUseCase getPatientMedicalHistoryUseCase;
    @MockitoBean private GetPatientMedicalHistoryDetailUseCase getPatientMedicalHistoryDetailUseCase;
    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private RoleRepository roleRepository;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private AuditLogRepository auditLogRepository;
    @MockitoBean private CurrentUserPort currentUserPort;

    @Test
    void listHistoryReturns200() throws Exception {
        UUID visitId = UUID.randomUUID();

        when(getPatientMedicalHistoryUseCase.getMedicalHistory())
                .thenReturn(List.of(new PatientMedicalHistorySummaryResult(
                        visitId,
                        Instant.parse("2099-01-10T02:00:00Z"),
                        "Dr. A",
                        "Internal Medicine",
                        "Hypertension",
                        2)));

        mockMvc.perform(get("/patient-portal/medical-history")
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitId").value(visitId.toString()))
                .andExpect(jsonPath("$[0].doctorName").value("Dr. A"))
                .andExpect(jsonPath("$[0].prescriptionCount").value(2));
    }

    @Test
    void getDetailReturns200() throws Exception {
        UUID visitId = UUID.randomUUID();

        when(getPatientMedicalHistoryDetailUseCase.getMedicalHistoryDetail(visitId))
                .thenReturn(new PatientMedicalHistoryDetailResult(
                        visitId,
                        Instant.parse("2099-01-10T02:00:00Z"),
                        "Dr. A",
                        "Internal Medicine",
                        List.of(new PatientMedicalHistoryDetailResult.DiagnosisItem("I10", "Essential hypertension")),
                        List.of(new PatientMedicalHistoryDetailResult.PrescriptionItemView(
                                "Paracetamol", 10, "500mg", "After meals")),
                        "Rest and hydrate"));

        mockMvc.perform(get("/patient-portal/medical-history/{visitId}", visitId)
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnoses[0].icd10Code").value("I10"))
                .andExpect(jsonPath("$.prescriptionItems[0].medicineName").value("Paracetamol"))
                .andExpect(jsonPath("$.doctorAdvice").value("Rest and hydrate"));
    }

    @Test
    void getDetailReturns403ForOtherPatient() throws Exception {
        UUID visitId = UUID.randomUUID();

        when(getPatientMedicalHistoryDetailUseCase.getMedicalHistoryDetail(visitId))
                .thenThrow(new AccessDeniedException("Patient may only access their own data."));

        mockMvc.perform(get("/patient-portal/medical-history/{visitId}", visitId)
                        .with(user("patient").roles("PATIENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}

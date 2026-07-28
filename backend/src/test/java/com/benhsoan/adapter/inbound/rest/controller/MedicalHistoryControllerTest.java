package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalHistoryRestMapper;
import com.benhsoan.application.ucservice.queries.medicalrecord.GetPatientMedicalHistoryQueryHandler;
import com.benhsoan.domain.patient.Visit;
import com.benhsoan.domain.patient.enums.VisitStatus;
import com.benhsoan.domain.patient.enums.VisitType;
import com.benhsoan.domain.patient.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicalHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MedicalHistoryRestMapper.class)
@DisplayName("MedicalHistoryController - MockMvc Tests")
class MedicalHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPatientMedicalHistoryQueryHandler handler;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private ClockPort clockPort;

    private final UUID patientId = UUID.randomUUID();

    @Test
    @DisplayName("GET /patients/{patientId}/medical-history should return 200 OK")
    void getMedicalHistoryReturns200() throws Exception {

        Visit visit = Visit.restore(
                UUID.randomUUID(),
                patientId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "V001",
                VisitType.OUTPATIENT,
                VisitStatus.COMPLETED,
                Instant.now(),
                "Headache",
                "Prescribed medicine",
                Instant.now(),
                Instant.now()
        );

        Page<Visit> page = new PageImpl<>(Collections.singletonList(visit));

        when(handler.handle(any())).thenReturn(page);

        mockMvc.perform(get("/patients/{patientId}/medical-history", patientId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].visitCode").value("V001"))
                .andExpect(jsonPath("$.content[0].reason").value("Headache"))
                .andExpect(jsonPath("$.content[0].visitStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.content[0].visitType").value("OUTPATIENT"));
    }

    @Test
    @DisplayName("GET /patients/{patientId}/medical-history with date filters should return 200 OK")
    void getMedicalHistoryWithDateFiltersReturns200() throws Exception {

        when(handler.handle(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/patients/{patientId}/medical-history", patientId)
                        .param("fromDate", "2026-01-01T00:00:00Z")
                        .param("toDate", "2026-12-31T23:59:59Z")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /patients/{patientId}/medical-history should return 403 when access denied")
    void getMedicalHistoryReturns403WhenAccessDenied() throws Exception {

        when(handler.handle(any()))
                .thenThrow(new MedicalRecordAccessDeniedException(
                        "You do not have permission to view "
                                + "this patient's medical history"));

        mockMvc.perform(get("/patients/{patientId}/medical-history", patientId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /patients/{patientId}/medical-history should return 404 when patient not found")
    void getMedicalHistoryReturns404WhenPatientNotFound() throws Exception {

        when(handler.handle(any()))
                .thenThrow(new com.benhsoan.domain.patient.exception
                        .PatientNotFoundException(patientId));

        mockMvc.perform(get("/patients/{patientId}/medical-history", patientId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isNotFound());
    }
}

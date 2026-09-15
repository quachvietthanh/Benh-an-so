package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientFamilyHistoryRestMapper;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;
import com.benhsoan.port.inbound.patient.AddPatientFamilyHistoryUseCase;
import com.benhsoan.port.inbound.patient.DeletePatientFamilyHistoryUseCase;
import com.benhsoan.port.inbound.patient.GetPatientFamilyHistoryUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientFamilyHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PatientFamilyHistoryRestMapper.class, GlobalExceptionHandler.class})
class PatientFamilyHistoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AddPatientFamilyHistoryUseCase addPatientFamilyHistoryUseCase;
    @MockitoBean private GetPatientFamilyHistoryUseCase getPatientFamilyHistoryUseCase;
    @MockitoBean private DeletePatientFamilyHistoryUseCase deletePatientFamilyHistoryUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void createsFamilyHistory() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID historyId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        PatientFamilyHistoryResult result = PatientFamilyHistoryResult.builder()
                .id(historyId)
                .patientId(patientId)
                .relationship("Bố")
                .diagnosisCatalogId(catalogId)
                .active(true)
                .createdAt(Instant.parse("2026-09-10T10:00:00Z"))
                .updatedAt(Instant.parse("2026-09-10T10:00:00Z"))
                .build();
        when(addPatientFamilyHistoryUseCase.addFamilyHistory(any())).thenReturn(result);

        mockMvc.perform(post("/patients/{patientId}/family-history", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relationship\":\"Bố\",\"diagnosisCatalogId\":\"" + catalogId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(historyId.toString()))
                .andExpect(jsonPath("$.relationship").value("Bố"));
    }

    @Test
    void rejectsBlankRelationship() throws Exception {
        mockMvc.perform(post("/patients/{patientId}/family-history", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relationship\":\" \",\"diagnosisCatalogId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingDiagnosisCatalogId() throws Exception {
        mockMvc.perform(post("/patients/{patientId}/family-history", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relationship\":\"Bố\"}"))
                .andExpect(status().isBadRequest());
    }
}

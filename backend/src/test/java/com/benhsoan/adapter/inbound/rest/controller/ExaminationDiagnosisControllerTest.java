package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.ExaminationDiagnosisRestMapper;
import com.benhsoan.port.dto.result.ExaminationDiagnosisResult;
import com.benhsoan.port.inbound.medicalrecord.GetExaminationDiagnosisUseCase;
import com.benhsoan.port.inbound.medicalrecord.RecordDiagnosisUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = ExaminationDiagnosisController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ExaminationDiagnosisRestMapper.class)
@DisplayName("ExaminationDiagnosisController - MockMvc Tests")
class ExaminationDiagnosisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecordDiagnosisUseCase recordDiagnosisUseCase;

    @MockitoBean
    private GetExaminationDiagnosisUseCase getExaminationDiagnosisUseCase;

    @MockitoBean
    private CurrentUserPort currentUserPort;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserSessionRepository userSessionRepository;

    @MockitoBean
    private JwtTokenPort jwtTokenPort;

    @MockitoBean
    private ClockPort clockPort;

    private final UUID examinationId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();

    @Test
    @DisplayName("POST /examinations/{id}/diagnosis - 200 OK")
    void recordDiagnosisReturns200() throws Exception {
        when(recordDiagnosisUseCase.recordDiagnosis(eq(examinationId), any()))
                .thenReturn(new ExaminationDiagnosisResult(
                        UUID.randomUUID(), examinationId, doctorId,
                        "J00", "Common cold", List.of(), "Notes",
                        Instant.now(), List.of()));

        String body = """
                {
                    "primaryIcdCode": "J00",
                    "primaryIcdName": "Common cold"
                }
                """;

        mockMvc.perform(post("/examinations/{examinationId}/diagnosis", examinationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryIcdCode").value("J00"))
                .andExpect(jsonPath("$.primaryIcdName").value("Common cold"))
                .andExpect(jsonPath("$.visitId").value(examinationId.toString()));
    }

    @Test
    @DisplayName("POST /examinations/{id}/diagnosis - 400 when missing required fields")
    void recordDiagnosisMissingFields() throws Exception {
        String body = """
                {
                    "clinicalNotes": "Some notes"
                }
                """;

        mockMvc.perform(post("/examinations/{examinationId}/diagnosis", examinationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /examinations/{id}/diagnosis - 200 OK")
    void getDiagnosisReturns200() throws Exception {
        when(getExaminationDiagnosisUseCase.getDiagnosis(examinationId))
                .thenReturn(new ExaminationDiagnosisResult(
                        UUID.randomUUID(), examinationId, doctorId,
                        "J00", "Common cold", List.of(), "Notes",
                        Instant.now(), List.of()));

        mockMvc.perform(get("/examinations/{examinationId}/diagnosis", examinationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitId").value(examinationId.toString()))
                .andExpect(jsonPath("$.primaryIcdCode").value("J00"));
    }

}

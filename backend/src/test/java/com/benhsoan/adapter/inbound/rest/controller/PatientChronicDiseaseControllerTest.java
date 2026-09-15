package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.PatientChronicDiseaseRestMapper;
import com.benhsoan.domain.patient.exception.PatientChronicDiseaseNotFoundException;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.port.dto.command.patient.DeletePatientChronicDiseaseCommand;
import com.benhsoan.port.dto.result.patient.PatientChronicDiseaseResult;
import com.benhsoan.port.inbound.patient.AddPatientChronicDiseaseUseCase;
import com.benhsoan.port.inbound.patient.DeletePatientChronicDiseaseUseCase;
import com.benhsoan.port.inbound.patient.GetPatientChronicDiseasesUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientChronicDiseaseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PatientChronicDiseaseRestMapper.class, GlobalExceptionHandler.class})
class PatientChronicDiseaseControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AddPatientChronicDiseaseUseCase addPatientChronicDiseaseUseCase;
    @MockitoBean private GetPatientChronicDiseasesUseCase getPatientChronicDiseasesUseCase;
    @MockitoBean private DeletePatientChronicDiseaseUseCase deletePatientChronicDiseaseUseCase;

    @MockitoBean private JwtTokenPort jwtTokenPort;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserSessionRepository userSessionRepository;
    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    @Test
    void createsChronicDisease() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID diseaseId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        PatientChronicDiseaseResult result = PatientChronicDiseaseResult.builder()
                .id(diseaseId)
                .patientId(patientId)
                .diagnosisCatalogId(catalogId)
                .yearDetected(2015)
                .active(true)
                .createdAt(Instant.parse("2026-09-10T10:00:00Z"))
                .updatedAt(Instant.parse("2026-09-10T10:00:00Z"))
                .build();
        when(addPatientChronicDiseaseUseCase.addChronicDisease(any())).thenReturn(result);

        mockMvc.perform(post("/patients/{patientId}/chronic-diseases", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosisCatalogId\":\"" + catalogId + "\",\"yearDetected\":2015}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(diseaseId.toString()))
                .andExpect(jsonPath("$.diagnosisCatalogId").value(catalogId.toString()))
                .andExpect(jsonPath("$.yearDetected").value(2015));
    }

    @Test
    void rejectsMissingDiagnosisCatalogId() throws Exception {
        mockMvc.perform(post("/patients/{patientId}/chronic-diseases", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"yearDetected\":2015}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsChronicDiseases() throws Exception {
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
        when(getPatientChronicDiseasesUseCase.getChronicDiseases(patientId)).thenReturn(List.of(result));

        mockMvc.perform(get("/patients/{patientId}/chronic-diseases", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosisCatalogId").value(catalogId.toString()));
    }

    @Test
    void deletesChronicDisease() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID diseaseId = UUID.randomUUID();

        mockMvc.perform(delete("/patients/{patientId}/chronic-diseases/{chronicDiseaseId}", patientId, diseaseId)
                        .param("reason", "Đã khỏi bệnh"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DeletePatientChronicDiseaseCommand> captor =
                ArgumentCaptor.forClass(DeletePatientChronicDiseaseCommand.class);
        verify(deletePatientChronicDiseaseUseCase).deleteChronicDisease(captor.capture());
        assertEquals(patientId, captor.getValue().patientId());
        assertEquals(diseaseId, captor.getValue().chronicDiseaseId());
        assertEquals("Đã khỏi bệnh", captor.getValue().reason());
    }

    @Test
    void mapsDeleteNotFoundTo404() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID diseaseId = UUID.randomUUID();
        doThrow(new PatientChronicDiseaseNotFoundException(diseaseId))
                .when(deletePatientChronicDiseaseUseCase).deleteChronicDisease(any());

        mockMvc.perform(delete("/patients/{patientId}/chronic-diseases/{chronicDiseaseId}", patientId, diseaseId))
                .andExpect(status().isNotFound());
    }
}

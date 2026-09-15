package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

import com.benhsoan.adapter.inbound.rest.mapper.PatientFamilyHistoryRestMapper;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.port.dto.command.patient.DeletePatientFamilyHistoryCommand;
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

    @Test
    void deletesFamilyHistory() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID historyId = UUID.randomUUID();

        mockMvc.perform(delete("/patients/{patientId}/family-history/{familyHistoryId}", patientId, historyId)
                        .param("reason", "Đã khỏi bệnh"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DeletePatientFamilyHistoryCommand> captor =
                ArgumentCaptor.forClass(DeletePatientFamilyHistoryCommand.class);
        verify(deletePatientFamilyHistoryUseCase).deleteFamilyHistory(captor.capture());
        assertEquals(patientId, captor.getValue().patientId());
        assertEquals(historyId, captor.getValue().familyHistoryId());
        assertEquals("Đã khỏi bệnh", captor.getValue().reason());
    }

    @Test
    void listsFamilyHistoryWithDiagnosisCodeAndName() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();
        PatientFamilyHistoryResult result = PatientFamilyHistoryResult.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .relationship("Bố")
                .diagnosisCatalogId(catalogId)
                .diagnosisCode("E11.9")
                .diagnosisName("Đái tháo đường type 2")
                .active(true)
                .createdAt(Instant.parse("2026-09-10T10:00:00Z"))
                .updatedAt(Instant.parse("2026-09-10T10:00:00Z"))
                .build();
        when(getPatientFamilyHistoryUseCase.getFamilyHistory(patientId)).thenReturn(List.of(result));

        mockMvc.perform(get("/patients/{patientId}/family-history", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relationship").value("Bố"))
                .andExpect(jsonPath("$[0].diagnosisCode").value("E11.9"))
                .andExpect(jsonPath("$[0].diagnosisName").value("Đái tháo đường type 2"));
    }
}

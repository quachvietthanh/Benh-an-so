package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalHistoryRestMapper;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalHistoryItemResult;
import com.benhsoan.port.inbound.patient.ViewPatientMedicalHistoryUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicalHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MedicalHistoryRestMapper.class)
class MedicalHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ViewPatientMedicalHistoryUseCase viewPatientMedicalHistoryUseCase;
    @MockitoBean
    private JwtTokenPort jwtTokenPort;
    @MockitoBean
    private UserSessionRepository userSessionRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private CurrentUserPort currentUserPort;
    @MockitoBean
    private ClockPort clockPort;

    @Test
    void returnsPagedMedicalHistory() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        MedicalHistoryItemResult item = new MedicalHistoryItemResult(
                visitId, "VIS-001", VisitType.WALK_IN, VisitStatus.COMPLETED,
                Instant.parse("2026-08-20T02:00:00Z"), null, Instant.parse("2026-08-20T03:00:00Z"),
                "Consultation", null, UUID.randomUUID(), "Dr. An", UUID.randomUUID(),
                MedicalRecordStatus.LOCKED, "Headache", "Recovered"
        );
        when(viewPatientMedicalHistoryUseCase.viewMedicalHistory(any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(item)));

        mockMvc.perform(get("/medical-history/patients/{patientId}", patientId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].visitId").value(visitId.toString()))
                .andExpect(jsonPath("$.content[0].doctorName").value("Dr. An"))
                .andExpect(jsonPath("$.content[0].medicalRecordStatus").value("LOCKED"));

        verify(viewPatientMedicalHistoryUseCase).viewMedicalHistory(any());
    }

    @Test
    void rejectsInvalidDateRange() throws Exception {
        mockMvc.perform(get("/medical-history/patients/{patientId}", UUID.randomUUID())
                        .param("from", "2026-08-31T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }
}

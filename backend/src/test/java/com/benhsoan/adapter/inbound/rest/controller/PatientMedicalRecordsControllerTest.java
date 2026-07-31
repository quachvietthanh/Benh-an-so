package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDetailRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.PatientRestMapper;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.patient.RegisterPatientUseCase;
import com.benhsoan.port.inbound.patient.SearchPatientUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = PatientController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ PatientRestMapper.class, MedicalRecordDetailRestMapper.class })
@DisplayName("PatientController /medical-records - MockMvc Tests")
class PatientMedicalRecordsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterPatientUseCase registerPatientUseCase;
    @MockitoBean
    private SearchPatientUseCase searchPatientUseCase;
    @MockitoBean
    private UpdatePatientUseCase updatePatientUseCase;
    @MockitoBean
    private GetMedicalRecordUseCase getMedicalRecordUseCase;

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

    private final UUID visitId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final Instant now = Instant.now();

    private MedicalRecordDetailResult detailResult() {
        return new MedicalRecordDetailResult(
                new MedicalRecordDetailResult.PatientInfo(patientId, "BN-0001", "Nguyen Van A",
                        LocalDate.of(1990, 1, 1), Gender.MALE, "0900000000", "ID-1", "BH-1"),
                new MedicalRecordDetailResult.VisitInfo(visitId, "VS-0001", VisitType.WALK_IN,
                        VisitStatus.COMPLETED, now, now, now, "Exam", null, doctorId, "Dr. Tran B"),
                recordId, "Headache", "Pain", "None", "Normal", "Stable", "Rest",
                "Follow-up", "Migraine", MedicalRecordStatus.OPEN, null, null,
                "G43", "Migraine", List.of("J00"), List.of());
    }

    @Test
    @DisplayName("GET /patients/{patientId}/medical-records - 200 OK with history list")
    void getPatientMedicalRecordsReturnsHistory() throws Exception {
        when(getMedicalRecordUseCase.getHistoryByPatientId(patientId))
                .thenReturn(List.of(detailResult()));

        mockMvc.perform(get("/patients/{patientId}/medical-records", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicalRecordId").value(recordId.toString()))
                .andExpect(jsonPath("$[0].patient.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$[0].visit.visitCode").value("VS-0001"))
                .andExpect(jsonPath("$[0].primaryIcdCode").value("G43"))
                .andExpect(jsonPath("$[0].primaryIcdName").value("Migraine"));
    }

    @Test
    @DisplayName("GET /patients/{patientId}/medical-records - empty list when no records")
    void getPatientMedicalRecordsReturnsEmptyList() throws Exception {
        when(getMedicalRecordUseCase.getHistoryByPatientId(patientId))
                .thenReturn(List.of());

        mockMvc.perform(get("/patients/{patientId}/medical-records", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}

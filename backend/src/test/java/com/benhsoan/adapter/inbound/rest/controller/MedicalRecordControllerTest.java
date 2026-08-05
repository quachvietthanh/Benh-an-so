package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDiagnosisRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordRestMapper;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult;
import com.benhsoan.port.dto.result.MedicalRecordDiagnosisResult;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordAccessLogsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.LockMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ReplaceMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.outbound.authSecurity.JwtTokenPort;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@WebMvcTest(controllers = MedicalRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ MedicalRecordRestMapper.class, MedicalRecordDetailRestMapper.class, MedicalRecordDiagnosisRestMapper.class })
@DisplayName("MedicalRecordController - MockMvc Tests")
class MedicalRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateMedicalRecordUseCase createMedicalRecordUseCase;
    @MockitoBean
    private GetMedicalRecordUseCase getMedicalRecordUseCase;
    @MockitoBean
    private UpdateMedicalRecordUseCase updateMedicalRecordUseCase;
    @MockitoBean
    private LockMedicalRecordUseCase lockMedicalRecordUseCase;
    @MockitoBean
    private AmendMedicalRecordUseCase amendMedicalRecordUseCase;
    @MockitoBean
    private GetMedicalRecordAccessLogsUseCase getMedicalRecordAccessLogsUseCase;
    @MockitoBean
    private GetMedicalRecordDiagnosesUseCase getMedicalRecordDiagnosesUseCase;
    @MockitoBean
    private ReplaceMedicalRecordDiagnosesUseCase replaceMedicalRecordDiagnosesUseCase;

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
    @DisplayName("GET /medical-records/visits/{visitId} - 200 OK with detail content")
    void getByVisitIdReturnsDetail() throws Exception {
        when(getMedicalRecordUseCase.getDetailByVisitId(visitId)).thenReturn(detailResult());

        mockMvc.perform(get("/medical-records/visits/{visitId}", visitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicalRecordId").value(recordId.toString()))
                .andExpect(jsonPath("$.patient.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.patient.gender").value("MALE"))
                .andExpect(jsonPath("$.visit.visitCode").value("VS-0001"))
                .andExpect(jsonPath("$.visit.doctorName").value("Dr. Tran B"))
                .andExpect(jsonPath("$.chiefComplaint").value("Headache"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.primaryIcdCode").value("G43"))
                .andExpect(jsonPath("$.primaryIcdName").value("Migraine"))
                .andExpect(jsonPath("$.secondaryIcdCodes[0]").value("J00"));
    }

    @Test
    @DisplayName("GET /medical-records/patient/{patientId} - 200 OK with history list")
    void getPatientMedicalRecordsReturnsHistory() throws Exception {
        when(getMedicalRecordUseCase.getHistoryByPatientId(patientId))
                .thenReturn(List.of(detailResult()));

        mockMvc.perform(get("/medical-records/patient/{patientId}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicalRecordId").value(recordId.toString()))
                .andExpect(jsonPath("$[0].patient.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$[0].visit.visitCode").value("VS-0001"))
                .andExpect(jsonPath("$[0].primaryIcdCode").value("G43"))
                .andExpect(jsonPath("$[0].primaryIcdName").value("Migraine"));
    }

    @Test
    @DisplayName("GET /medical-records/patient/{patientId} - empty list when no records")
    void getPatientMedicalRecordsReturnsEmptyList() throws Exception {
        when(getMedicalRecordUseCase.getHistoryByPatientId(patientId))
                .thenReturn(List.of());

        mockMvc.perform(get("/medical-records/patient/{patientId}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("PUT /medical-records/{id}/diagnoses - replaces persisted diagnosis list")
    void replaceDiagnosesReturnsPersistedResponses() throws Exception {
        UUID catalogId = UUID.randomUUID();
        when(replaceMedicalRecordDiagnosesUseCase.replace(org.mockito.ArgumentMatchers.eq(recordId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(new MedicalRecordDiagnosisResult(UUID.randomUUID(), recordId, "J06.9",
                        "Acute upper respiratory infection", com.benhsoan.domain.medicalrecord.enums.DiagnosisType.PRIMARY,
                        "Monitor symptoms", doctorId, now)));

        mockMvc.perform(put("/medical-records/{medicalRecordId}/diagnoses", recordId)
                        .contentType("application/json")
                        .content("""
                                {"primaryDiagnosis":{"diagnosisCatalogId":"%s","code":"J06.9","name":"Acute upper respiratory infection","note":"Monitor symptoms"},"secondaryDiagnoses":[]}
                                """.formatted(catalogId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicalRecordId").value(recordId.toString()))
                .andExpect(jsonPath("$[0].diagnosisType").value("PRIMARY"));
    }
}

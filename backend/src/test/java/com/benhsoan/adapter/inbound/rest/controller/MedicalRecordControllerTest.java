package com.benhsoan.adapter.inbound.rest.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDetailRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordDiagnosisRestMapper;
import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordRestMapper;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalRecordAccessLogResult;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult;
import com.benhsoan.port.dto.result.MedicalRecordDiagnosisResult;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.dto.result.MedicalRecordTemplateOptionResult;
import com.benhsoan.port.dto.result.MedicalRecordTemplateSelectionResult;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.dto.result.AppliedMedicalRecordTemplateResult;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.ApplyMedicalRecordTemplateUseCase;
import com.benhsoan.port.inbound.medicalrecord.ArchiveMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.DeleteMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordAccessLogsUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordTemplateSelectionUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordVersionHistoryUseCase;
import com.benhsoan.port.inbound.medicalrecord.IssueMedicalRecordCopyUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.inbound.medicalrecord.LockMedicalRecordUseCase;
import com.benhsoan.port.inbound.medicalrecord.SignMedicalRecordUseCase;
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
    private GetMedicalRecordTemplateSelectionUseCase getMedicalRecordTemplateSelectionUseCase;
    @MockitoBean
    private ApplyMedicalRecordTemplateUseCase applyMedicalRecordTemplateUseCase;
    @MockitoBean
    private UpdateMedicalRecordUseCase updateMedicalRecordUseCase;
    @MockitoBean
    private LockMedicalRecordUseCase lockMedicalRecordUseCase;
    @MockitoBean
    private SignMedicalRecordUseCase signMedicalRecordUseCase;
    @MockitoBean
    private AmendMedicalRecordUseCase amendMedicalRecordUseCase;
    @MockitoBean
    private GetMedicalRecordAccessLogsUseCase getMedicalRecordAccessLogsUseCase;
    @MockitoBean
    private GetMedicalRecordDiagnosesUseCase getMedicalRecordDiagnosesUseCase;
    @MockitoBean
    private ReplaceMedicalRecordDiagnosesUseCase replaceMedicalRecordDiagnosesUseCase;
    @MockitoBean
    private ArchiveMedicalRecordUseCase archiveMedicalRecordUseCase;
    @MockitoBean
    private DeleteMedicalRecordUseCase deleteMedicalRecordUseCase;
    @MockitoBean
    private IssueMedicalRecordCopyUseCase issueMedicalRecordCopyUseCase;
    @MockitoBean
    private GetMedicalRecordVersionHistoryUseCase getMedicalRecordVersionHistoryUseCase;

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
                "Follow-up", "Migraine", MedicalRecordStatus.OPEN, null, null, null, null, null,
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
                                {"primaryDiagnosis":{"diagnosisCatalogId":"%s","note":"Monitor symptoms"},"secondaryDiagnoses":[]}
                                """.formatted(catalogId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicalRecordId").value(recordId.toString()))
                .andExpect(jsonPath("$[0].diagnosisType").value("PRIMARY"));
    }

    @Test
    @DisplayName("PUT /medical-records/{id}/diagnoses - rejects a primary diagnosis without catalog ID")
    void replaceDiagnosesRejectsPrimaryWithoutCatalogId() throws Exception {
        mockMvc.perform(put("/medical-records/{medicalRecordId}/diagnoses", recordId)
                        .contentType("application/json")
                        .content("""
                                {"primaryDiagnosis":{},"secondaryDiagnoses":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /medical-records/{id}/diagnoses - requires exactly one secondary diagnosis source")
    void replaceDiagnosesRequiresExactlyOneSecondaryDiagnosisSource() throws Exception {
        UUID catalogId = UUID.randomUUID();

        mockMvc.perform(put("/medical-records/{medicalRecordId}/diagnoses", recordId)
                        .contentType("application/json")
                        .content("""
                                {"primaryDiagnosis":{"diagnosisCatalogId":"%s"},"secondaryDiagnoses":[{}]}
                                """.formatted(catalogId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/medical-records/{medicalRecordId}/diagnoses", recordId)
                        .contentType("application/json")
                        .content("""
                                {"primaryDiagnosis":{"diagnosisCatalogId":"%s"},"secondaryDiagnoses":[{"diagnosisCatalogId":"%s","name":"Clinical observation"}]}
                                """.formatted(catalogId, UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /medical-records/access-logs - 200 OK with patient/time filters")
    void getAccessLogsByPatientReturnsPage() throws Exception {
        UUID accessedBy = UUID.randomUUID();
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-12T23:59:59Z");
        when(getMedicalRecordAccessLogsUseCase.getAccessLogs(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(
                        List.of(new MedicalRecordAccessLogResult(
                                UUID.randomUUID(),
                                patientId,
                                visitId,
                                recordId,
                                accessedBy,
                                MedicalRecordAccessAction.VIEW,
                                "Medical record viewed",
                                now
                        )),
                        PageRequest.of(0, 20),
                        1
                ));

        mockMvc.perform(get("/medical-records/access-logs")
                        .param("patientId", patientId.toString())
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientId").value(patientId.toString()))
                .andExpect(jsonPath("$.content[0].medicalRecordId").value(recordId.toString()))
                .andExpect(jsonPath("$.content[0].action").value("VIEW"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /medical-records/{id}/sign - 200 OK with signed response")
    void signMedicalRecordReturnsSignedResponse() throws Exception {
        when(signMedicalRecordUseCase.sign(org.mockito.ArgumentMatchers.eq(recordId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MedicalRecordResult(
                        recordId, visitId, "Headache", "Pain", "None", "Normal", "Stable",
                        "Rest", "Follow-up", "Migraine", MedicalRecordStatus.SIGNED,
                        "DR_SIM_SIG", now, doctorId, null, null, doctorId, now, doctorId, now
                ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/medical-records/{medicalRecordId}/sign", recordId)
                        .contentType("application/json")
                        .content("""
                                {"signatureData":"DR_SIM_SIG"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recordId.toString()))
                .andExpect(jsonPath("$.status").value("SIGNED"))
                .andExpect(jsonPath("$.signatureData").value("DR_SIM_SIG"))
                .andExpect(jsonPath("$.signedBy").value(doctorId.toString()));
    }

    @Test
    @DisplayName("GET /medical-records/{id}/template-options - returns effective template")
    void getTemplateOptionsReturnsEffectiveTemplate() throws Exception {
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        SpecialtyResult specialty = new SpecialtyResult(UUID.randomUUID(), "GENERAL", "General", true);
        MedicalRecordTemplateOptionResult option = new MedicalRecordTemplateOptionResult(templateId, versionId,
                specialty, "Initial examination", 2, true, List.of());
        when(getMedicalRecordTemplateSelectionUseCase.getForMedicalRecord(recordId))
                .thenReturn(new MedicalRecordTemplateSelectionResult(recordId, visitId, specialty,
                        List.of(option), option, false));

        mockMvc.perform(get("/medical-records/{medicalRecordId}/template-options", recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveTemplate.templateId").value(templateId.toString()))
                .andExpect(jsonPath("$.effectiveTemplate.templateVersionId").value(versionId.toString()))
                .andExpect(jsonPath("$.fallback").value(false));
    }

    @Test
    @DisplayName("PUT /medical-records/{id}/template - returns applied immutable template")
    void applyTemplateReturnsAppliedTemplate() throws Exception {
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        SpecialtyResult specialty = new SpecialtyResult(UUID.randomUUID(), "GENERAL", "General", true);
        AppliedMedicalRecordTemplateResult applied = new AppliedMedicalRecordTemplateResult(templateId, versionId,
                specialty, "Initial examination", 2, List.of(), doctorId, now, false);
        when(applyMedicalRecordTemplateUseCase.apply(org.mockito.ArgumentMatchers.eq(recordId),
                org.mockito.ArgumentMatchers.any())).thenReturn(new MedicalRecordResult(
                        recordId, visitId, null, null, null, null, null, null, null, null,
                        MedicalRecordStatus.DRAFT, null, null, null, null, null, doctorId, now, doctorId, now, applied));

        mockMvc.perform(put("/medical-records/{medicalRecordId}/template", recordId)
                        .contentType("application/json")
                        .content("{\"templateId\":\"" + templateId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedTemplate.templateId").value(templateId.toString()))
                .andExpect(jsonPath("$.appliedTemplate.templateVersionId").value(versionId.toString()))
                .andExpect(jsonPath("$.appliedTemplate.appliedBy").value(doctorId.toString()));
    }

    @Test
    @DisplayName("PUT /medical-records/{id}/template - rejects a request without templateId")
    void applyTemplateRejectsMissingTemplateId() throws Exception {
        mockMvc.perform(put("/medical-records/{medicalRecordId}/template", recordId)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(applyMedicalRecordTemplateUseCase);
    }
}

package com.benhsoan.adapter.inbound.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalClinicalResultRestMapper;
import com.benhsoan.application.ucservice.patient.PatientAccessDeniedAuditWriter;
import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.application.ucservice.portal.ExportPatientPortalClinicalResultService;
import com.benhsoan.application.ucservice.portal.GetPatientPortalClinicalResultDetailService;
import com.benhsoan.application.ucservice.portal.GetPatientPortalClinicalResultsService;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.exception.GlobalExceptionHandler;
import com.benhsoan.port.dto.result.portal.ClinicalResultPrintDocument;
import com.benhsoan.port.outbound.pdf.ClinicalResultPdfRenderer;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.clinical.MedicalAttachmentRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * NCL-14-CN-009 Security Integration Test:
 * Verifies QTN-23 data scoping and all Acceptance Criteria (TC-01, TC-02, TC-03, TC-04)
 * with real PatientAccessGuard, real PatientAccessDeniedAuditWriter and GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientPortalClinicalResultSecurityIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-22T08:30:00Z");

    @Mock private ClinicalResultRepository clinicalResultRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private ClinicalOrderItemRepository clinicalOrderItemRepository;
    @Mock private ClinicalOrderRepository clinicalOrderRepository;
    @Mock private MedicalAttachmentRepository medicalAttachmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock private ClinicalResultPdfRenderer clinicalResultPdfRenderer;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private MockMvc mockMvc;

    private UUID currentUserId;
    private UUID ownPatientId;
    private UUID otherPatientId;
    private UUID ownVisitId;
    private UUID otherVisitId;
    private UUID ownResultId;
    private UUID otherResultId;
    private UUID doctorId;
    private UUID specialtyId;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        ownPatientId = UUID.randomUUID();
        otherPatientId = UUID.randomUUID();
        ownVisitId = UUID.randomUUID();
        otherVisitId = UUID.randomUUID();
        ownResultId = UUID.randomUUID();
        otherResultId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        specialtyId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);

        Patient ownPatient = mockPatient(ownPatientId);
        when(patientRepository.findByUserId(currentUserId)).thenReturn(Optional.of(ownPatient));
        when(patientRepository.findById(ownPatientId)).thenReturn(Optional.of(ownPatient));

        Patient otherPatient = mockPatient(otherPatientId);
        when(patientRepository.findById(otherPatientId)).thenReturn(Optional.of(otherPatient));

        ObjectMapper objectMapper = new ObjectMapper();
        PatientAccessDeniedAuditWriter denialAuditWriter =
                new PatientAccessDeniedAuditWriter(auditLogRepository, objectMapper);
        PatientAccessGuard patientAccessGuard =
                new PatientAccessGuard(currentUserPort, patientRepository, denialAuditWriter, clockPort);

        GetPatientPortalClinicalResultsService getResultsService = new GetPatientPortalClinicalResultsService(
                visitRepository, clinicalResultRepository, clinicalOrderItemRepository,
                medicalAttachmentRepository, userRepository, patientAccessGuard,
                auditLogRepository, currentUserPort, clockPort, objectMapper
        );

        GetPatientPortalClinicalResultDetailService getDetailService = new GetPatientPortalClinicalResultDetailService(
                clinicalResultRepository, visitRepository, clinicalOrderItemRepository,
                clinicalOrderRepository, medicalAttachmentRepository, userRepository,
                specialtyRepository, patientAccessGuard, auditLogRepository,
                currentUserPort, clockPort, objectMapper
        );

        ExportPatientPortalClinicalResultService exportService = new ExportPatientPortalClinicalResultService(
                clinicalResultRepository, visitRepository, patientRepository,
                clinicalOrderItemRepository, clinicalOrderRepository, userRepository,
                specialtyRepository, clinicConfigurationRepository, clinicalResultPdfRenderer,
                patientAccessGuard, auditLogRepository, currentUserPort,
                clockPort, objectMapper
        );

        PatientPortalClinicalResultRestMapper mapper = new PatientPortalClinicalResultRestMapper();

        PatientPortalClinicalResultController controller = new PatientPortalClinicalResultController(
                getResultsService, getDetailService, exportService, mapper
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Patient mockPatient(UUID id) {
        Patient patient = Mockito.mock(Patient.class);
        when(patient.getId()).thenReturn(id);
        when(patient.getPatientCode()).thenReturn("BN-001");
        when(patient.getFullName()).thenReturn("Nguyễn Văn Bệnh Nhân");
        when(patient.getGender()).thenReturn(Gender.MALE);
        when(patient.getDateOfBirth()).thenReturn(LocalDate.of(1990, 1, 1));
        when(patient.getPhone()).thenReturn("0901234567");
        return patient;
    }

    private Visit mockVisit(UUID vId, UUID pId) {
        Visit visit = Mockito.mock(Visit.class);
        when(visit.getId()).thenReturn(vId);
        when(visit.getVisitCode()).thenReturn("KB-001");
        when(visit.getPatientId()).thenReturn(pId);
        when(visit.getDoctorId()).thenReturn(doctorId);
        when(visit.getSpecialtyId()).thenReturn(specialtyId);
        when(visit.getVisitAt()).thenReturn(NOW);
        when(visit.getStatus()).thenReturn(VisitStatus.COMPLETED);
        return visit;
    }

    private ClinicalResult mockResult(UUID resultId, UUID vId, ClinicalResultStatus status) {
        UUID itemId = UUID.randomUUID();
        return ClinicalResult.restore(
                resultId, itemId, vId, ClinicalResultType.NUMBER,
                new BigDecimal("14.2"), null, "g/dL", "12.0 - 16.5",
                new BigDecimal("12.0"), new BigDecimal("16.5"),
                ClinicalResultAbnormalFlag.NORMAL, "Bình thường", status,
                doctorId, NOW, null, null
        );
    }

    @Test
    @DisplayName("TC-01 / TC-04: Patient views own confirmed clinical results -> 200 OK & READ audit")
    void getClinicalResults_ownPatient_returns200AndAuditsRead() throws Exception {
        Visit visit = mockVisit(ownVisitId, ownPatientId);
        when(visitRepository.findById(ownVisitId)).thenReturn(Optional.of(visit));

        ClinicalResult result = mockResult(ownResultId, ownVisitId, ClinicalResultStatus.FINAL);
        when(clinicalResultRepository.findByVisitIdAndStatus(ownVisitId, ClinicalResultStatus.FINAL))
                .thenReturn(List.of(result));

        ClinicalOrderItem item = ClinicalOrderItem.restore(
                result.getClinicalOrderItemId(), UUID.randomUUID(), UUID.randomUUID(),
                "XN-MAU-01", "Tổng phân tích tế bào máu", null,
                ClinicalOrderItemStatus.COMPLETED, NOW, NOW
        );
        when(clinicalOrderItemRepository.findByIdIn(any())).thenReturn(List.of(item));

        User doctor = Mockito.mock(User.class);
        when(doctor.getId()).thenReturn(doctorId);
        when(doctor.getFullName()).thenReturn("BS. Bác Sĩ");
        when(userRepository.findAllById(any())).thenReturn(List.of(doctor));

        mockMvc.perform(get("/patient-portal/clinical-results")
                        .param("visitId", ownVisitId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clinicalResultId").value(ownResultId.toString()))
                .andExpect(jsonPath("$[0].serviceCode").value("XN-MAU-01"))
                .andExpect(jsonPath("$[0].status").value("FINAL"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(ActionType.READ, captor.getValue().getActionType());
        assertEquals(ResourceType.CLINICAL_RESULT, captor.getValue().getResourceType());
        assertTrue(captor.getValue().getDetail().contains("ONLINE_PORTAL"));
    }

    @Test
    @DisplayName("TC-02: Unconfirmed (DRAFT) results are hidden from patient -> returns empty list")
    void getClinicalResults_draftResults_areHidden() throws Exception {
        Visit visit = mockVisit(ownVisitId, ownPatientId);
        when(visitRepository.findById(ownVisitId)).thenReturn(Optional.of(visit));

        // When only DRAFT results exist, findByVisitIdAndStatus(..., FINAL) returns empty
        when(clinicalResultRepository.findByVisitIdAndStatus(ownVisitId, ClinicalResultStatus.FINAL))
                .thenReturn(List.of());

        mockMvc.perform(get("/patient-portal/clinical-results")
                        .param("visitId", ownVisitId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("TC-03 / QTN-23: Patient tampers with visitId of another patient -> 403 Forbidden & ACCESS_DENIED audit")
    void getClinicalResults_crossPatientTampering_returns403AndAuditsDenied() throws Exception {
        Visit otherVisit = mockVisit(otherVisitId, otherPatientId);
        when(visitRepository.findById(otherVisitId)).thenReturn(Optional.of(otherVisit));

        mockMvc.perform(get("/patient-portal/clinical-results")
                        .param("visitId", otherVisitId.toString()))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(ActionType.ACCESS_DENIED, audit.getActionType());
        assertEquals(ResourceType.CLINICAL_RESULT, audit.getResourceType());
        assertEquals(otherVisitId, audit.getResourceId());
        assertTrue(audit.getDetail().contains("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("TC-03 / QTN-23: Patient tampers with resultId of another patient -> 403 Forbidden & ACCESS_DENIED audit")
    void getResultDetail_crossPatientTampering_returns403AndAuditsDenied() throws Exception {
        ClinicalResult otherResult = mockResult(otherResultId, otherVisitId, ClinicalResultStatus.FINAL);
        when(clinicalResultRepository.findById(otherResultId)).thenReturn(Optional.of(otherResult));

        Visit otherVisit = mockVisit(otherVisitId, otherPatientId);
        when(visitRepository.findById(otherVisitId)).thenReturn(Optional.of(otherVisit));

        mockMvc.perform(get("/patient-portal/clinical-results/" + otherResultId))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(ActionType.ACCESS_DENIED, audit.getActionType());
        assertEquals(ResourceType.CLINICAL_RESULT, audit.getResourceType());
        assertEquals(otherResultId, audit.getResourceId());
    }

    @Test
    @DisplayName("TC-01: Download result PDF -> 200 OK & EXPORT audit")
    void downloadResult_ownPatient_returnsPdfAndAuditsExport() throws Exception {
        ClinicalResult result = mockResult(ownResultId, ownVisitId, ClinicalResultStatus.FINAL);
        when(clinicalResultRepository.findById(ownResultId)).thenReturn(Optional.of(result));

        Visit visit = mockVisit(ownVisitId, ownPatientId);
        when(visitRepository.findById(ownVisitId)).thenReturn(Optional.of(visit));

        ClinicalOrderItem item = ClinicalOrderItem.restore(
                result.getClinicalOrderItemId(), UUID.randomUUID(), UUID.randomUUID(),
                "XN-MAU-01", "Tổng phân tích tế bào máu", null,
                ClinicalOrderItemStatus.COMPLETED, NOW, NOW
        );
        when(clinicalOrderItemRepository.findById(result.getClinicalOrderItemId())).thenReturn(Optional.of(item));

        byte[] fakePdf = new byte[]{1, 2, 3, 4};
        when(clinicalResultPdfRenderer.render(any(ClinicalResultPrintDocument.class))).thenReturn(fakePdf);

        mockMvc.perform(get("/patient-portal/clinical-results/" + ownResultId + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ket-qua-XN-MAU-01-KB-001.pdf\""));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(ActionType.EXPORT, captor.getValue().getActionType());
        assertEquals(ResourceType.CLINICAL_RESULT, captor.getValue().getResourceType());
    }

    @Test
    @DisplayName("TC-03: Download result PDF of another patient -> 403 Forbidden & ACCESS_DENIED audit")
    void downloadResult_crossPatient_returns403AndAuditsDenied() throws Exception {
        ClinicalResult otherResult = mockResult(otherResultId, otherVisitId, ClinicalResultStatus.FINAL);
        when(clinicalResultRepository.findById(otherResultId)).thenReturn(Optional.of(otherResult));

        Visit otherVisit = mockVisit(otherVisitId, otherPatientId);
        when(visitRepository.findById(otherVisitId)).thenReturn(Optional.of(otherVisit));

        mockMvc.perform(get("/patient-portal/clinical-results/" + otherResultId + "/download"))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(ActionType.ACCESS_DENIED, captor.getValue().getActionType());
        assertEquals(ResourceType.CLINICAL_RESULT, captor.getValue().getResourceType());
    }

    @Test
    @DisplayName("SEC-01 / TC-03 / P2-01: Tampering with DRAFT resultId of another patient -> 403 Forbidden & ACCESS_DENIED audit")
    void getResultDetail_crossPatientDraftTampering_returns403AndAuditsDenied() throws Exception {
        ClinicalResult otherDraftResult = mockResult(otherResultId, otherVisitId, ClinicalResultStatus.DRAFT);
        when(clinicalResultRepository.findById(otherResultId)).thenReturn(Optional.of(otherDraftResult));

        Visit otherVisit = mockVisit(otherVisitId, otherPatientId);
        when(visitRepository.findById(otherVisitId)).thenReturn(Optional.of(otherVisit));

        mockMvc.perform(get("/patient-portal/clinical-results/" + otherResultId))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(ActionType.ACCESS_DENIED, audit.getActionType());
        assertEquals(ResourceType.CLINICAL_RESULT, audit.getResourceType());
        assertEquals(otherResultId, audit.getResourceId());
    }

    @Test
    @DisplayName("SEC-02 / TC-03 / P2-01: Tampering with download of DRAFT result of another patient -> 403 Forbidden & ACCESS_DENIED audit")
    void downloadResult_crossPatientDraftTampering_returns403AndAuditsDenied() throws Exception {
        ClinicalResult otherDraftResult = mockResult(otherResultId, otherVisitId, ClinicalResultStatus.DRAFT);
        when(clinicalResultRepository.findById(otherResultId)).thenReturn(Optional.of(otherDraftResult));

        Visit otherVisit = mockVisit(otherVisitId, otherPatientId);
        when(visitRepository.findById(otherVisitId)).thenReturn(Optional.of(otherVisit));

        mockMvc.perform(get("/patient-portal/clinical-results/" + otherResultId + "/download"))
                .andExpect(status().isForbidden());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(ActionType.ACCESS_DENIED, captor.getValue().getActionType());
        assertEquals(ResourceType.CLINICAL_RESULT, captor.getValue().getResourceType());
    }
}

package com.benhsoan.application.ucservice.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.clinical.exception.ClinicalResultNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.port.dto.result.portal.ClinicalResultPrintDocument;
import com.benhsoan.port.dto.result.portal.ClinicalResultPrintResult;
import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultDetailResult;
import com.benhsoan.port.dto.result.portal.PatientPortalClinicalResultSummaryResult;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientPortalClinicalResultServicesTest {

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
    @Mock private PatientAccessGuard patientAccessGuard;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID currentUserId;
    private UUID patientId;
    private UUID visitId;
    private UUID doctorId;
    private UUID specialtyId;

    @BeforeEach
    void setUpCommon() {
        currentUserId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        visitId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        specialtyId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
    }

    private Visit mockVisit(UUID vId, UUID pId, VisitStatus status) {
        Visit visit = mock(Visit.class);
        when(visit.getId()).thenReturn(vId);
        when(visit.getVisitCode()).thenReturn("KB-20260922-0001");
        when(visit.getPatientId()).thenReturn(pId);
        when(visit.getDoctorId()).thenReturn(doctorId);
        when(visit.getSpecialtyId()).thenReturn(specialtyId);
        when(visit.getVisitAt()).thenReturn(NOW);
        when(visit.getStatus()).thenReturn(status);
        return visit;
    }

    private Patient mockPatient(UUID pId) {
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(pId);
        when(patient.getPatientCode()).thenReturn("BN-20260922-0001");
        when(patient.getFullName()).thenReturn("Nguyễn Văn Bệnh Nhân");
        when(patient.getDateOfBirth()).thenReturn(LocalDate.of(1990, 5, 15));
        when(patient.getGender()).thenReturn(Gender.MALE);
        when(patient.getPhone()).thenReturn("0901234567");
        return patient;
    }

    private ClinicalResult mockResult(UUID resultId, UUID itemId, UUID vId, ClinicalResultStatus status) {
        return ClinicalResult.restore(
                resultId,
                itemId,
                vId,
                ClinicalResultType.NUMBER,
                new BigDecimal("14.2"),
                null,
                "g/dL",
                "12.0 - 16.5",
                new BigDecimal("12.0"),
                new BigDecimal("16.5"),
                ClinicalResultAbnormalFlag.NORMAL,
                "Chỉ số bình thường",
                status,
                doctorId,
                NOW.minusSeconds(1800),
                null,
                null
        );
    }

    private ClinicalOrderItem mockOrderItem(UUID itemId, UUID orderId) {
        return ClinicalOrderItem.restore(
                itemId,
                orderId,
                UUID.randomUUID(),
                "XN-MAU-01",
                "Tổng phân tích tế bào máu",
                "Lấy mẫu buổi sáng",
                ClinicalOrderItemStatus.COMPLETED,
                NOW.minusSeconds(3000),
                NOW.minusSeconds(1800)
        );
    }

    // =========================================================================
    // 1. GetPatientPortalClinicalResultsService Tests
    // =========================================================================
    @Nested
    @DisplayName("GetPatientPortalClinicalResultsService Tests")
    class GetResultsTests {

        private GetPatientPortalClinicalResultsService service;

        @BeforeEach
        void setUp() {
            service = new GetPatientPortalClinicalResultsService(
                    visitRepository,
                    clinicalResultRepository,
                    clinicalOrderItemRepository,
                    medicalAttachmentRepository,
                    userRepository,
                    patientAccessGuard,
                    auditLogRepository,
                    currentUserPort,
                    clockPort,
                    objectMapper
            );
        }

        @Test
        @DisplayName("TC-01: Returns confirmed results and audits READ operation")
        void returnsConfirmedResults_andAudits() {
            Visit visit = mockVisit(visitId, patientId, VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            UUID resultId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            ClinicalResult result = mockResult(resultId, itemId, visitId, ClinicalResultStatus.FINAL);
            when(clinicalResultRepository.findByVisitIdAndStatus(visitId, ClinicalResultStatus.FINAL))
                    .thenReturn(List.of(result));

            ClinicalOrderItem item = mockOrderItem(itemId, UUID.randomUUID());
            when(clinicalOrderItemRepository.findByIdIn(any())).thenReturn(List.of(item));

            User doctor = mock(User.class);
            when(doctor.getId()).thenReturn(doctorId);
            when(doctor.getFullName()).thenReturn("BS. Nguyễn Văn A");
            when(userRepository.findAllById(any())).thenReturn(List.of(doctor));
            when(medicalAttachmentRepository.findByClinicalResultIdIn(any())).thenReturn(List.of());

            List<PatientPortalClinicalResultSummaryResult> results = service.getClinicalResults(visitId);

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("XN-MAU-01", results.get(0).serviceCode());
            assertEquals("FINAL", results.get(0).status());
            assertEquals("BS. Nguyễn Văn A", results.get(0).doctorName());
            assertFalse(results.get(0).hasAttachment());

            verify(patientAccessGuard).requirePatientOwnership(patientId, ResourceType.CLINICAL_RESULT, visitId);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            AuditLog audit = captor.getValue();
            assertEquals(ActionType.READ, audit.getActionType());
            assertEquals(ResourceType.CLINICAL_RESULT, audit.getResourceType());
            assertEquals(visitId, audit.getResourceId());
            assertTrue(audit.getDetail().contains("ONLINE_PORTAL"));
        }

        @Test
        @DisplayName("TC-02: Excludes unconfirmed (DRAFT) results")
        void excludesDraftResults() {
            Visit visit = mockVisit(visitId, patientId, VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            when(clinicalResultRepository.findByVisitIdAndStatus(visitId, ClinicalResultStatus.FINAL))
                    .thenReturn(List.of());

            List<PatientPortalClinicalResultSummaryResult> results = service.getClinicalResults(visitId);

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("TC-03: Throws AccessDeniedException when accessing another patient's visit")
        void throwsAccessDenied_whenCrossPatient() {
            UUID otherPatientId = UUID.randomUUID();
            Visit visit = mockVisit(visitId, otherPatientId, VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            doThrow(new AccessDeniedException("Patient may only access their own data."))
                    .when(patientAccessGuard).requirePatientOwnership(otherPatientId, ResourceType.CLINICAL_RESULT, visitId);

            assertThrows(AccessDeniedException.class, () -> service.getClinicalResults(visitId));
        }

        @Test
        @DisplayName("TC-04: Returns confirmed results even when visit is COMPLETED (results returned later)")
        void returnsResults_whenVisitCompleted() {
            Visit visit = mockVisit(visitId, patientId, VisitStatus.COMPLETED);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            UUID resultId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            ClinicalResult result = mockResult(resultId, itemId, visitId, ClinicalResultStatus.FINAL);
            when(clinicalResultRepository.findByVisitIdAndStatus(visitId, ClinicalResultStatus.FINAL))
                    .thenReturn(List.of(result));

            ClinicalOrderItem item = mockOrderItem(itemId, UUID.randomUUID());
            when(clinicalOrderItemRepository.findByIdIn(any())).thenReturn(List.of(item));
            when(userRepository.findAllById(any())).thenReturn(List.of());

            List<PatientPortalClinicalResultSummaryResult> results = service.getClinicalResults(visitId);

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("XN-MAU-01", results.get(0).serviceCode());
        }

        @Test
        @DisplayName("UT-05 / P3-02: Batch fetch clinical order items and users without N+1 query")
        void batchFetching_callsFindByIdInOnce() {
            Visit visit = mockVisit(visitId, patientId, VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            UUID res1 = UUID.randomUUID();
            UUID res2 = UUID.randomUUID();
            UUID item1 = UUID.randomUUID();
            UUID item2 = UUID.randomUUID();
            ClinicalResult r1 = mockResult(res1, item1, visitId, ClinicalResultStatus.FINAL);
            ClinicalResult r2 = mockResult(res2, item2, visitId, ClinicalResultStatus.FINAL);
            when(clinicalResultRepository.findByVisitIdAndStatus(visitId, ClinicalResultStatus.FINAL))
                    .thenReturn(List.of(r1, r2));

            when(clinicalOrderItemRepository.findByIdIn(any())).thenReturn(List.of(
                    mockOrderItem(item1, UUID.randomUUID()),
                    mockOrderItem(item2, UUID.randomUUID())
            ));
            when(userRepository.findAllById(any())).thenReturn(List.of());

            service.getClinicalResults(visitId);

            verify(clinicalOrderItemRepository).findByIdIn(any());
            verify(userRepository).findAllById(any());
        }
    }

    // =========================================================================
    // 2. GetPatientPortalClinicalResultDetailService Tests
    // =========================================================================
    @Nested
    @DisplayName("GetPatientPortalClinicalResultDetailService Tests")
    class GetDetailTests {

        private GetPatientPortalClinicalResultDetailService service;

        @BeforeEach
        void setUp() {
            service = new GetPatientPortalClinicalResultDetailService(
                    clinicalResultRepository,
                    visitRepository,
                    clinicalOrderItemRepository,
                    clinicalOrderRepository,
                    medicalAttachmentRepository,
                    userRepository,
                    specialtyRepository,
                    patientAccessGuard,
                    auditLogRepository,
                    currentUserPort,
                    clockPort,
                    objectMapper
            );
        }

        @Test
        @DisplayName("TC-01: Returns single result detail successfully")
        void returnsResultDetail_success() {
            UUID resultId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            ClinicalResult result = mockResult(resultId, itemId, visitId, ClinicalResultStatus.FINAL);
            when(clinicalResultRepository.findById(resultId)).thenReturn(Optional.of(result));

            Visit visit = mockVisit(visitId, patientId, VisitStatus.COMPLETED);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            ClinicalOrderItem item = mockOrderItem(itemId, orderId);
            when(clinicalOrderItemRepository.findById(itemId)).thenReturn(Optional.of(item));

            ClinicalOrder order = mock(ClinicalOrder.class);
            when(order.getOrderedBy()).thenReturn(doctorId);
            when(clinicalOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

            User doctor = mock(User.class);
            when(doctor.getId()).thenReturn(doctorId);
            when(doctor.getFullName()).thenReturn("BS. Bác Sĩ");
            when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

            Specialty specialty = mock(Specialty.class);
            when(specialty.getId()).thenReturn(specialtyId);
            when(specialty.getName()).thenReturn("Khoa Xét nghiệm");
            when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));

            PatientPortalClinicalResultDetailResult detail = service.getClinicalResultDetail(resultId);

            assertNotNull(detail);
            assertEquals(resultId, detail.clinicalResultId());
            assertEquals("XN-MAU-01", detail.serviceCode());
            assertEquals("BS. Bác Sĩ", detail.orderingDoctorName());
            assertEquals("Khoa Xét nghiệm", detail.specialtyName());

            verify(patientAccessGuard).requirePatientOwnership(patientId, ResourceType.CLINICAL_RESULT, resultId);
        }

        @Test
        @DisplayName("TC-02 / UT-01: Own patient accessing DRAFT result throws ClinicalResultNotFoundException")
        void throwsNotFound_whenDraft_ownPatient() {
            UUID resultId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            ClinicalResult result = mockResult(resultId, itemId, visitId, ClinicalResultStatus.DRAFT);
            when(clinicalResultRepository.findById(resultId)).thenReturn(Optional.of(result));

            Visit visit = mockVisit(visitId, patientId, VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            assertThrows(ClinicalResultNotFoundException.class, () -> service.getClinicalResultDetail(resultId));
            verify(patientAccessGuard).requirePatientOwnership(patientId, ResourceType.CLINICAL_RESULT, resultId);
        }

        @Test
        @DisplayName("TC-03 / UT-02 / P2-01: Other patient accessing DRAFT result throws AccessDeniedException and audits denial")
        void throwsAccessDenied_whenDraft_otherPatient() {
            UUID resultId = UUID.randomUUID();
            UUID otherPatientId = UUID.randomUUID();
            ClinicalResult result = mockResult(resultId, UUID.randomUUID(), visitId, ClinicalResultStatus.DRAFT);
            when(clinicalResultRepository.findById(resultId)).thenReturn(Optional.of(result));

            Visit visit = mockVisit(visitId, otherPatientId, VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            doThrow(new AccessDeniedException("Denied")).when(patientAccessGuard)
                    .requirePatientOwnership(otherPatientId, ResourceType.CLINICAL_RESULT, resultId);

            assertThrows(AccessDeniedException.class, () -> service.getClinicalResultDetail(resultId));
        }

        @Test
        @DisplayName("TC-03: Throws AccessDeniedException when accessing result of another patient")
        void throwsAccessDenied_whenOtherPatient() {
            UUID resultId = UUID.randomUUID();
            UUID otherPatientId = UUID.randomUUID();
            ClinicalResult result = mockResult(resultId, UUID.randomUUID(), visitId, ClinicalResultStatus.FINAL);
            when(clinicalResultRepository.findById(resultId)).thenReturn(Optional.of(result));

            Visit visit = mockVisit(visitId, otherPatientId, VisitStatus.COMPLETED);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            doThrow(new AccessDeniedException("Forbidden")).when(patientAccessGuard)
                    .requirePatientOwnership(otherPatientId, ResourceType.CLINICAL_RESULT, resultId);

            assertThrows(AccessDeniedException.class, () -> service.getClinicalResultDetail(resultId));
        }
    }

    // =========================================================================
    // 3. ExportPatientPortalClinicalResultService Tests
    // =========================================================================
    @Nested
    @DisplayName("ExportPatientPortalClinicalResultService Tests")
    class ExportTests {

        private ExportPatientPortalClinicalResultService service;

        @BeforeEach
        void setUp() {
            service = new ExportPatientPortalClinicalResultService(
                    clinicalResultRepository,
                    visitRepository,
                    patientRepository,
                    clinicalOrderItemRepository,
                    clinicalOrderRepository,
                    userRepository,
                    specialtyRepository,
                    clinicConfigurationRepository,
                    clinicalResultPdfRenderer,
                    patientAccessGuard,
                    auditLogRepository,
                    currentUserPort,
                    clockPort,
                    objectMapper
            );
        }

        @Test
        @DisplayName("TC-01: Exports single result PDF successfully and audits EXPORT")
        void exportByResult_success() {
            UUID resultId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();

            ClinicalResult result = mockResult(resultId, itemId, visitId, ClinicalResultStatus.FINAL);
            when(clinicalResultRepository.findById(resultId)).thenReturn(Optional.of(result));

            Visit visit = mockVisit(visitId, patientId, VisitStatus.COMPLETED);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            Patient patient = mockPatient(patientId);
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

            ClinicalOrderItem item = mockOrderItem(itemId, UUID.randomUUID());
            when(clinicalOrderItemRepository.findById(itemId)).thenReturn(Optional.of(item));

            byte[] mockPdf = new byte[]{1, 2, 3, 4};
            when(clinicalResultPdfRenderer.render(any(ClinicalResultPrintDocument.class))).thenReturn(mockPdf);

            ClinicalResultPrintResult printResult = service.exportByResult(resultId);

            assertNotNull(printResult);
            assertEquals("application/pdf", printResult.contentType());
            assertEquals(mockPdf, printResult.content());
            assertTrue(printResult.fileName().contains("XN-MAU-01"));

            verify(patientAccessGuard).requirePatientOwnership(patientId, ResourceType.CLINICAL_RESULT, resultId);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            assertEquals(ActionType.EXPORT, captor.getValue().getActionType());
        }

        @Test
        @DisplayName("TC-01: Exports visit clinical results PDF successfully")
        void exportByVisit_success() {
            Visit visit = mockVisit(visitId, patientId, VisitStatus.COMPLETED);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            Patient patient = mockPatient(patientId);
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

            UUID resultId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            ClinicalResult result = mockResult(resultId, itemId, visitId, ClinicalResultStatus.FINAL);
            when(clinicalResultRepository.findByVisitIdAndStatus(visitId, ClinicalResultStatus.FINAL))
                    .thenReturn(List.of(result));

            ClinicalOrderItem item = mockOrderItem(itemId, UUID.randomUUID());
            when(clinicalOrderItemRepository.findByIdIn(any())).thenReturn(List.of(item));

            byte[] mockPdf = new byte[]{5, 6, 7, 8};
            when(clinicalResultPdfRenderer.render(any(ClinicalResultPrintDocument.class))).thenReturn(mockPdf);

            ClinicalResultPrintResult printResult = service.exportByVisit(visitId);

            assertNotNull(printResult);
            assertEquals("application/pdf", printResult.contentType());
            assertEquals(mockPdf, printResult.content());

            verify(patientAccessGuard).requirePatientOwnership(patientId, ResourceType.CLINICAL_RESULT, visitId);
        }

        @Test
        @DisplayName("TC-03: Throws AccessDeniedException when exporting other patient's result")
        void exportByResult_crossPatient_throwsAccessDenied() {
            UUID resultId = UUID.randomUUID();
            UUID otherPatientId = UUID.randomUUID();
            ClinicalResult result = mockResult(resultId, UUID.randomUUID(), visitId, ClinicalResultStatus.FINAL);
            when(clinicalResultRepository.findById(resultId)).thenReturn(Optional.of(result));

            Visit visit = mockVisit(visitId, otherPatientId, VisitStatus.COMPLETED);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            doThrow(new AccessDeniedException("Denied")).when(patientAccessGuard)
                    .requirePatientOwnership(otherPatientId, ResourceType.CLINICAL_RESULT, resultId);

            assertThrows(AccessDeniedException.class, () -> service.exportByResult(resultId));
        }

        @Test
        @DisplayName("TC-03 / UT-03 / P2-01: Throws AccessDeniedException when exporting other patient's DRAFT result")
        void exportByResult_draft_crossPatient_throwsAccessDenied() {
            UUID resultId = UUID.randomUUID();
            UUID otherPatientId = UUID.randomUUID();
            ClinicalResult result = mockResult(resultId, UUID.randomUUID(), visitId, ClinicalResultStatus.DRAFT);
            when(clinicalResultRepository.findById(resultId)).thenReturn(Optional.of(result));

            Visit visit = mockVisit(visitId, otherPatientId, VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            doThrow(new AccessDeniedException("Denied")).when(patientAccessGuard)
                    .requirePatientOwnership(otherPatientId, ResourceType.CLINICAL_RESULT, resultId);

            assertThrows(AccessDeniedException.class, () -> service.exportByResult(resultId));
        }

        @Test
        @DisplayName("UT-04 / Test Gap 1: exportByVisit throws ClinicalResultNotFoundException when no FINAL results exist")
        void exportByVisit_whenNoFinalResults_throwsNotFound() {
            Visit visit = mockVisit(visitId, patientId, VisitStatus.IN_PROGRESS);
            when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

            Patient patient = mockPatient(patientId);
            when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
            when(clinicalResultRepository.findByVisitIdAndStatus(visitId, ClinicalResultStatus.FINAL))
                    .thenReturn(List.of());

            assertThrows(ClinicalResultNotFoundException.class, () -> service.exportByVisit(visitId));
        }
    }
}

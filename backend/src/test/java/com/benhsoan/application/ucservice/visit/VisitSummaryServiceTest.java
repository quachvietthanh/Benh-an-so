package com.benhsoan.application.ucservice.visit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotSignedException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;
import com.benhsoan.port.dto.result.VisitSummaryPrintDocument;
import com.benhsoan.port.dto.result.VisitSummaryPrintResult;
import com.benhsoan.port.dto.result.VisitSummaryResult;
import com.benhsoan.port.outbound.pdf.VisitSummaryPdfRenderer;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAccessLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class VisitSummaryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:00:00Z");
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID CURRENT_USER_ID = UUID.randomUUID();

    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock private ClinicalOrderRepository clinicalOrderRepository;
    @Mock private ClinicalOrderItemRepository clinicalOrderItemRepository;
    @Mock private MedicalRecordAccessLogRepository accessLogRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private MedicalRecordAccessAuditService accessAuditService;
    @Mock private VisitSummaryPdfRenderer pdfRenderer;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private AnonymizationModeState anonymizationModeState;
    private ObjectMapper objectMapper;
    private VisitSummaryService service;

    private Visit visit;
    private MedicalRecord signedRecord;
    private Patient patient;
    private User doctor;
    private ClinicConfiguration clinic;

    @BeforeEach
    void setUp() {
        anonymizationModeState = new AnonymizationModeState();
        objectMapper = new ObjectMapper();
        service = new VisitSummaryService(
                visitRepository, medicalRecordRepository, patientRepository, userRepository,
                clinicConfigurationRepository, medicalRecordDiagnosisRepository, clinicalOrderRepository,
                clinicalOrderItemRepository, accessLogRepository, auditLogRepository,
                accessAuditService, pdfRenderer, currentUserPort, clockPort,
                anonymizationModeState, objectMapper
        );

        visit = Visit.restore(
                VISIT_ID, "KB-20260820-0001", PATIENT_ID, DOCTOR_ID, null, null,
                VisitType.WALK_IN, VisitStatus.COMPLETED, NOW.minusSeconds(3600), NOW.minusSeconds(3000), NOW,
                "Đau đầu, sốt nhẹ", "Theo dõi ngoại trú", DOCTOR_ID, NOW.minusSeconds(3600), NOW
        );

        signedRecord = MedicalRecord.restore(
                RECORD_ID, VISIT_ID, "Sốt và đau đầu", "Mệt mỏi, ho khan", "Không có",
                "Họng đỏ nhẹ", "Ổn định", "Uống thuốc theo đơn", "Nghỉ ngơi, uống nhiều nước",
                "Viêm mũi họng cấp", LocalDate.of(2026, 8, 27), MedicalRecordStatus.SIGNED,
                "SIGNATURE_DATA_MOCK", NOW.minusSeconds(600), DOCTOR_ID,
                null, null, DOCTOR_ID, NOW.minusSeconds(3000), DOCTOR_ID, NOW.minusSeconds(600),
                null, null, null
        );

        patient = Patient.restore(
                PATIENT_ID, "BN-2026-0001", "Nguyễn Văn A", LocalDate.of(1990, 5, 15),
                Gender.MALE, "0901234567", null, null, "012345678901", null,
                BloodType.UNKNOWN, null, null, true, NOW, null, null, DOCTOR_ID
        );

        doctor = User.restore(
                DOCTOR_ID, "doctor1", "hash", "BS. Trần Văn B", "doctor1@benhsoan.com",
                "0912345678", DOCTOR_ID, true, null, NOW
        );

        clinic = ClinicConfiguration.create(
                "Phòng khám Đa khoa Hoàn Mỹ", "123 Hoàng Văn Thụ, TP.HCM", "02838445566",
                java.time.LocalTime.of(8, 0), java.time.LocalTime.of(17, 0), NOW
        );
    }

    @Test
    void TC_01_getSummary_whenMedicalRecordSigned_returnsFullSummary() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.of(signedRecord));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinic));

        var diagnosis = MedicalRecordDiagnosis.restore(
                UUID.randomUUID(), RECORD_ID, UUID.randomUUID(), "J00", "Viêm mũi họng cấp",
                com.benhsoan.domain.medicalrecord.enums.DiagnosisType.PRIMARY, null, DOCTOR_ID, NOW, NOW, null
        );
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(List.of(diagnosis));

        var order = ClinicalOrder.restore(
                UUID.randomUUID(), "ORD-01", VISIT_ID, RECORD_ID, PATIENT_ID, DOCTOR_ID, "Kiểm tra",
                ClinicalOrderStatus.COMPLETED, NOW, NOW, NOW, NOW
        );
        when(clinicalOrderRepository.findByVisitId(eq(VISIT_ID), any())).thenReturn(new PageImpl<>(List.of(order)));

        var item = ClinicalOrderItem.restore(
                UUID.randomUUID(), order.getId(), UUID.randomUUID(), "XQ01", "X-Quang ngực",
                "Tư thế đứng", com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus.COMPLETED, NOW, null
        );
        when(clinicalOrderItemRepository.findByClinicalOrderIdIn(anyCollection())).thenReturn(List.of(item));

        when(accessLogRepository.search(any(GetMedicalRecordAccessLogsQuery.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        VisitSummaryResult result = service.getSummary(VISIT_ID);

        assertNotNull(result);
        assertEquals(VISIT_ID, result.visitId());
        assertEquals("KB-20260820-0001", result.visitCode());
        assertEquals("Nguyễn Văn A", result.patient().fullName());
        assertEquals("Phòng khám Đa khoa Hoàn Mỹ", result.clinic().name());
        assertEquals("BS. Trần Văn B", result.doctor().fullName());
        assertEquals(1, result.diagnoses().size());
        assertEquals("J00", result.diagnoses().get(0).code());
        assertEquals(1, result.clinicalOrders().size());
        assertEquals("ORD-01", result.clinicalOrders().get(0).orderCode());
        assertEquals("XQ01", result.clinicalOrders().get(0).serviceCode());
        assertEquals("X-Quang ngực", result.clinicalOrders().get(0).serviceName());
        assertEquals("Nghỉ ngơi, uống nhiều nước", result.doctorInstructions());
        assertEquals("Uống thuốc theo đơn", result.treatmentPlan());
        assertEquals(LocalDate.of(2026, 8, 27), result.revisitDate());
    }

    @Test
    void TC_02_getSummary_whenMedicalRecordNotSigned_throwsMedicalRecordNotSignedException() {
        MedicalRecord draftRecord = MedicalRecord.restore(
                RECORD_ID, VISIT_ID, "Lý do", "Triệu chứng", "Tiền sử",
                "Khám lâm sàng", "Diễn tiến", "Điều trị", "Lời dặn",
                "Kết luận", null, MedicalRecordStatus.DRAFT,
                null, null, null,
                null, null, DOCTOR_ID, NOW, DOCTOR_ID, NOW,
                null, null, null
        );

        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.of(draftRecord));

        MedicalRecordNotSignedException ex = assertThrows(
                MedicalRecordNotSignedException.class,
                () -> service.getSummary(VISIT_ID)
        );

        assertTrue(ex.getMessage().contains("chưa được ký"));
    }

    @Test
    void TC_02_export_whenMedicalRecordNotSigned_throwsMedicalRecordNotSignedException() {
        MedicalRecord draftRecord = MedicalRecord.restore(
                RECORD_ID, VISIT_ID, "Lý do", "Triệu chứng", "Tiền sử",
                "Khám lâm sàng", "Diễn tiến", "Điều trị", "Lời dặn",
                "Kết luận", null, MedicalRecordStatus.OPEN,
                null, null, null,
                null, null, DOCTOR_ID, NOW, DOCTOR_ID, NOW,
                null, null, null
        );

        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.of(draftRecord));

        assertThrows(MedicalRecordNotSignedException.class, () -> service.export(VISIT_ID));
        verify(pdfRenderer, never()).render(any());
        verify(auditLogRepository, never()).save(any());
        verify(accessAuditService, never()).recordRecordAccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getSummary_whenVisitNotFound_throwsVisitNotFoundException() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.empty());

        assertThrows(VisitNotFoundException.class, () -> service.getSummary(VISIT_ID));
    }

    @Test
    void getSummary_whenMedicalRecordNotFound_throwsMedicalRecordNotFoundException() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.empty());

        assertThrows(MedicalRecordNotFoundException.class, () -> service.getSummary(VISIT_ID));
    }

    @Test
    void TC_03_export_whenMedicalRecordSigned_rendersPdfAndWritesAudits() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.of(signedRecord));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(userRepository.findById(CURRENT_USER_ID)).thenReturn(Optional.of(doctor));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinic));
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(List.of());
        when(clinicalOrderRepository.findByVisitId(eq(VISIT_ID), any())).thenReturn(new PageImpl<>(List.of()));
        when(accessLogRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of()));

        when(currentUserPort.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
        when(clockPort.now()).thenReturn(NOW);

        byte[] fakePdf = new byte[]{1, 2, 3, 4};
        when(pdfRenderer.render(any(VisitSummaryPrintDocument.class))).thenReturn(fakePdf);

        VisitSummaryPrintResult printResult = service.export(VISIT_ID);

        assertNotNull(printResult);
        assertEquals("phieu-tom-tat-KB-20260820-0001.pdf", printResult.fileName());
        assertEquals("application/pdf", printResult.contentType());
        assertArrayEquals(fakePdf, printResult.content());

        // Kiểm tra TC-03: Ghi nhận AuditLog chung
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog savedAudit = auditCaptor.getValue();
        assertEquals(CURRENT_USER_ID, savedAudit.getUserId());
        assertEquals(ActionType.EXPORT, savedAudit.getActionType());
        assertEquals(ResourceType.VISIT, savedAudit.getResourceType());
        assertEquals(VISIT_ID, savedAudit.getResourceId());

        // Kiểm tra TC-03: Ghi nhận MedicalRecordAccessLog chuyên biệt với PRINT qua transaction độc lập
        verify(accessAuditService).recordRecordAccessInNewTransaction(
                eq(PATIENT_ID), eq(VISIT_ID), eq(RECORD_ID), eq(CURRENT_USER_ID),
                eq(MedicalRecordAccessAction.PRINT), eq("In phiếu tóm tắt lượt khám"), eq(NOW)
        );
    }

    @Test
    void getSummary_whenAnonymizationModeEnabled_masksPatientName() {
        anonymizationModeState.setEnabled(true);

        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.of(signedRecord));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinic));
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(List.of());
        when(clinicalOrderRepository.findByVisitId(eq(VISIT_ID), any())).thenReturn(new PageImpl<>(List.of()));
        when(accessLogRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of()));

        VisitSummaryResult result = service.getSummary(VISIT_ID);

        assertEquals("BỆNH NHÂN #BN-2026-0001", result.patient().fullName());
    }

    @Test
    void getSummary_sortsPrimaryDiagnosisFirst() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.of(signedRecord));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinic));
        when(clinicalOrderRepository.findByVisitId(eq(VISIT_ID), any())).thenReturn(new PageImpl<>(List.of()));
        when(accessLogRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of()));

        var secondaryDiagnosis = MedicalRecordDiagnosis.restore(
                UUID.randomUUID(), RECORD_ID, UUID.randomUUID(), "R05", "Ho kéo dài",
                com.benhsoan.domain.medicalrecord.enums.DiagnosisType.SECONDARY, null, DOCTOR_ID, NOW, NOW, null
        );
        var primaryDiagnosis = MedicalRecordDiagnosis.restore(
                UUID.randomUUID(), RECORD_ID, UUID.randomUUID(), "J00", "Viêm mũi họng cấp",
                com.benhsoan.domain.medicalrecord.enums.DiagnosisType.PRIMARY, null, DOCTOR_ID, NOW, NOW, null
        );
        // Secondary returned first from repository
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID))
                .thenReturn(List.of(secondaryDiagnosis, primaryDiagnosis));

        VisitSummaryResult result = service.getSummary(VISIT_ID);

        assertEquals(2, result.diagnoses().size());
        assertEquals("J00", result.diagnoses().get(0).code());
        assertTrue(result.diagnoses().get(0).isPrimary());
        assertEquals("R05", result.diagnoses().get(1).code());
        assertFalse(result.diagnoses().get(1).isPrimary());
    }

    @Test
    void loadPrintHistory_filtersOnlyPrintAction_andSortsDescending() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.of(signedRecord));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinic));
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(List.of());
        when(clinicalOrderRepository.findByVisitId(eq(VISIT_ID), any())).thenReturn(new PageImpl<>(List.of()));

        Instant t1 = Instant.parse("2026-08-20T08:00:00Z");
        Instant t2 = Instant.parse("2026-08-20T08:30:00Z");
        Instant t3 = Instant.parse("2026-08-20T08:45:00Z");
        Instant t4 = Instant.parse("2026-08-20T09:00:00Z");

        var log1 = MedicalRecordAccessLog.createRecordAccess(PATIENT_ID, VISIT_ID, RECORD_ID, DOCTOR_ID, MedicalRecordAccessAction.VIEW, "Viewed", t1);
        var log2 = MedicalRecordAccessLog.createRecordAccess(PATIENT_ID, VISIT_ID, RECORD_ID, DOCTOR_ID, MedicalRecordAccessAction.PRINT, "Print 1", t2);
        var log3 = MedicalRecordAccessLog.createRecordAccess(PATIENT_ID, VISIT_ID, RECORD_ID, DOCTOR_ID, MedicalRecordAccessAction.SIGN, "Signed", t3);
        var log4 = MedicalRecordAccessLog.createRecordAccess(PATIENT_ID, VISIT_ID, RECORD_ID, DOCTOR_ID, MedicalRecordAccessAction.PRINT, "Print 2", t4);

        when(accessLogRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of(log1, log2, log3, log4)));

        VisitSummaryResult result = service.getSummary(VISIT_ID);

        assertEquals(2, result.printHistory().size());
        // Verify descending sort: t4 (09:00) before t2 (08:30)
        assertEquals(t4, result.printHistory().get(0).printedAt());
        assertEquals("Print 2", result.printHistory().get(0).detail());
        assertEquals(t2, result.printHistory().get(1).printedAt());
        assertEquals("Print 1", result.printHistory().get(1).detail());
        assertEquals("BS. Trần Văn B", result.printHistory().get(0).printedByName());
    }

    @Test
    void loadNonCancelledOrderItems_ignoresCancelledOrdersAndItems() {
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(VISIT_ID)).thenReturn(Optional.of(signedRecord));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinic));
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID)).thenReturn(List.of());
        when(accessLogRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of()));

        UUID orderCancelledId = UUID.randomUUID();
        var orderCancelled = ClinicalOrder.restore(
                orderCancelledId, "ORD-CANCELLED", VISIT_ID, RECORD_ID, PATIENT_ID, DOCTOR_ID, "Huy",
                ClinicalOrderStatus.CANCELLED, NOW, NOW, NOW, NOW
        );

        UUID orderActiveId = UUID.randomUUID();
        var orderActive = ClinicalOrder.restore(
                orderActiveId, "ORD-ACTIVE", VISIT_ID, RECORD_ID, PATIENT_ID, DOCTOR_ID, "Kham",
                ClinicalOrderStatus.COMPLETED, NOW, NOW, NOW, NOW
        );

        when(clinicalOrderRepository.findByVisitId(eq(VISIT_ID), any()))
                .thenReturn(new PageImpl<>(List.of(orderCancelled, orderActive)));

        var itemCompleted = ClinicalOrderItem.restore(
                UUID.randomUUID(), orderActiveId, UUID.randomUUID(), "XQ01", "X-Quang",
                "Chu chup", com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus.COMPLETED, NOW, null
        );
        var itemCancelled = ClinicalOrderItem.restore(
                UUID.randomUUID(), orderActiveId, UUID.randomUUID(), "SA01", "Sieu am",
                "Huy", com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus.CANCELLED, NOW, null
        );

        when(clinicalOrderItemRepository.findByClinicalOrderIdIn(anyCollection()))
                .thenReturn(List.of(itemCompleted, itemCancelled));

        VisitSummaryResult result = service.getSummary(VISIT_ID);

        assertEquals(1, result.clinicalOrders().size());
        assertEquals("ORD-ACTIVE", result.clinicalOrders().get(0).orderCode());
        assertEquals("XQ01", result.clinicalOrders().get(0).serviceCode());
        assertEquals("X-Quang", result.clinicalOrders().get(0).serviceName());
    }
}

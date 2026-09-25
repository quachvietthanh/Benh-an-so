package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.anonymization.AnonymizationModeState;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordMissingDiagnosisException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotSignedException;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.medicalrecord.ExportMedicalRecordExchangeCommand;
import com.benhsoan.port.dto.result.MedicalRecordExchangeExportResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;

@ExtendWith(MockitoExtension.class)
class ExportMedicalRecordExchangeServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T08:30:00Z");
    private static final UUID RECORD_ID_1 = UUID.randomUUID();
    private static final UUID RECORD_ID_2 = UUID.randomUUID();
    private static final UUID VISIT_ID_1 = UUID.randomUUID();
    private static final UUID VISIT_ID_2 = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClinicConfigurationRepository clinicConfigurationRepository;
    @Mock private ClinicalOrderRepository clinicalOrderRepository;
    @Mock private ClinicalOrderItemRepository clinicalOrderItemRepository;
    @Mock private ClinicalResultRepository clinicalResultRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private MedicineRepository medicineRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private MedicalRecordAccessAuditService accessAuditService;

    private ObjectMapper objectMapper;
    private AnonymizationModeState anonymizationModeState;
    private ExportMedicalRecordExchangeService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        anonymizationModeState = new AnonymizationModeState();
        service = new ExportMedicalRecordExchangeService(
                medicalRecordRepository,
                medicalRecordDiagnosisRepository,
                visitRepository,
                patientRepository,
                userRepository,
                clinicConfigurationRepository,
                clinicalOrderRepository,
                clinicalOrderItemRepository,
                clinicalResultRepository,
                prescriptionRepository,
                medicineRepository,
                currentUserPort,
                clockPort,
                auditLogRepository,
                accessAuditService,
                objectMapper,
                anonymizationModeState
        );
    }

    private void stubSecurityDefaults() {
        when(currentUserPort.hasPermission("MEDICAL_RECORD_EXPORT")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
    }

    private MedicalRecord createSignedRecord(UUID recordId, UUID visitId) {
        return MedicalRecord.restore(
                recordId,
                visitId,
                "Dau dau",
                "Dau nua dau",
                "Tien su THA",
                "HA 130/80",
                "On dinh",
                "Dieu tri noi khoa",
                "Uong thuoc deu dan",
                "Migraine",
                MedicalRecordStatus.SIGNED,
                "SIMULATED_SIGNATURE",
                NOW.minusSeconds(600),
                DOCTOR_ID,
                NOW.minusSeconds(600),
                DOCTOR_ID,
                DOCTOR_ID,
                NOW.minusSeconds(1000),
                DOCTOR_ID,
                NOW.minusSeconds(600)
        );
    }

    private MedicalRecord createDraftRecord(UUID recordId, UUID visitId) {
        return MedicalRecord.restore(
                recordId,
                visitId,
                "Dau dau",
                "Dau nua dau",
                "Tien su THA",
                "HA 130/80",
                "On dinh",
                "Dieu tri noi khoa",
                "Uong thuoc deu dan",
                "Migraine",
                MedicalRecordStatus.DRAFT,
                null,
                null,
                null,
                null,
                null,
                DOCTOR_ID,
                NOW.minusSeconds(1000),
                DOCTOR_ID,
                NOW.minusSeconds(600)
        );
    }

    private Visit createVisit(UUID visitId, String visitCode) {
        return Visit.restore(
                visitId,
                visitCode,
                PATIENT_ID,
                DOCTOR_ID,
                null,
                null,
                VisitType.WALK_IN,
                VisitStatus.COMPLETED,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(1800),
                NOW.minusSeconds(600),
                "Kham tong quat",
                null,
                DOCTOR_ID,
                NOW.minusSeconds(3600),
                NOW.minusSeconds(600)
        );
    }

    private Patient createPatient() {
        return Patient.restore(
                PATIENT_ID,
                "BN-2026-0001",
                "Nguyen Van A",
                LocalDate.of(1985, 5, 15),
                Gender.MALE,
                "0901234567",
                null,
                "123 Le Loi, Quan 1, TP. HCM",
                "079085000123",
                "GD4790000000123",
                BloodType.UNKNOWN,
                null,
                null,
                true,
                NOW.minusSeconds(10000),
                null,
                null,
                DOCTOR_ID
        );
    }

    private User createDoctor() {
        return User.restore(
                DOCTOR_ID,
                "doctor1",
                "hash",
                "BS. Tran Van B",
                "doctor@benhsoan.com",
                "0909999999",
                UUID.randomUUID(),
                true,
                NOW.minusSeconds(10000),
                NOW.minusSeconds(10000)
        );
    }

    @Test
    @DisplayName("TC-01 & TC-03: Xuất đơn lẻ thành công 1 hồ sơ đủ 5 khối, ánh xạ chuẩn serviceCode/serviceName và đơn thuốc")
    void testExportSingleRecord_Success_WithAllFiveBlocks() throws Exception {
        stubSecurityDefaults();

        MedicalRecord record = createSignedRecord(RECORD_ID_1, VISIT_ID_1);
        when(medicalRecordRepository.findById(RECORD_ID_1)).thenReturn(Optional.of(record));

        List<MedicalRecordDiagnosis> diagnoses = List.of(
                MedicalRecordDiagnosis.create(RECORD_ID_1, UUID.randomUUID(), "G43", "Migraine", DiagnosisType.PRIMARY, "Chinh", DOCTOR_ID, NOW),
                MedicalRecordDiagnosis.create(RECORD_ID_1, UUID.randomUUID(), "I10", "Tang huyet ap", DiagnosisType.SECONDARY, "Phu", DOCTOR_ID, NOW)
        );
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_1)).thenReturn(diagnoses);

        Visit visit = createVisit(VISIT_ID_1, "VS-2026-0001");
        when(visitRepository.findById(VISIT_ID_1)).thenReturn(Optional.of(visit));

        Patient patient = createPatient();
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

        User doctor = createDoctor();
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

        ClinicConfiguration clinicConfig = ClinicConfiguration.create(
                "Phong kham Da khoa Tieu chuan", "123 Y Te", "0281234567",
                LocalTime.of(8, 0), LocalTime.of(17, 0), NOW
        );
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(clinicConfig));

        // Khối 3: Chỉ định cận lâm sàng
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID serviceCatalogId = UUID.randomUUID();
        ClinicalOrder order = ClinicalOrder.restore(
                orderId, "ORD-2026-0001", VISIT_ID_1, RECORD_ID_1, PATIENT_ID, DOCTOR_ID,
                "Checkup", ClinicalOrderStatus.COMPLETED, NOW.minusSeconds(1000), NOW.minusSeconds(500),
                NOW.minusSeconds(1000), NOW.minusSeconds(500)
        );
        when(clinicalOrderRepository.findByVisitId(eq(VISIT_ID_1), any())).thenReturn(new PageImpl<>(List.of(order)));

        ClinicalOrderItem orderItem = ClinicalOrderItem.restore(
                orderItemId, orderId, serviceCatalogId, "XN-CTM", "Tong phan tich te bao mau",
                "Lay mau luc doi", ClinicalOrderItemStatus.COMPLETED, NOW.minusSeconds(1000), NOW.minusSeconds(500)
        );
        when(clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(orderId))).thenReturn(List.of(orderItem));

        // Khối 4: Kết quả cận lâm sàng
        UUID resultId = UUID.randomUUID();
        ClinicalResult result = ClinicalResult.restore(
                resultId, orderItemId, VISIT_ID_1, ClinicalResultType.NUMBER, BigDecimal.valueOf(142.0),
                null, "g/L", "130.0 - 170.0", ClinicalResultAbnormalFlag.NORMAL, "Binh thuong",
                ClinicalResultStatus.FINAL, DOCTOR_ID, NOW.minusSeconds(400), null, null
        );
        when(clinicalResultRepository.findByVisitId(eq(VISIT_ID_1), any())).thenReturn(new PageImpl<>(List.of(result)));

        // Khối 5: Đơn thuốc
        UUID prescriptionId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        PrescriptionItem rxItem = PrescriptionItem.restore(
                UUID.randomUUID(), prescriptionId, medicineId, "Paracetamol 500mg", "Paracetamol",
                "500mg", "Vien", "1 vien", 2, AdministrationRoute.ORAL, 5, 10,
                "Uong sau an khi dau", NOW.minusSeconds(200), null
        );
        Prescription prescription = Prescription.restore(
                prescriptionId, "RX-2026-0001", RECORD_ID_1, PrescriptionStatus.DISPENSED,
                "Uong dung gio", DOCTOR_ID, NOW.minusSeconds(200), null, null, List.of(rxItem)
        );
        when(prescriptionRepository.findByMedicalRecordId(RECORD_ID_1)).thenReturn(List.of(prescription));

        com.benhsoan.domain.medicine.Medicine medicine = com.benhsoan.domain.medicine.Medicine.create(
                medicineId, "MED-001", "Paracetamol 500mg", "Paracetamol", "500mg",
                com.benhsoan.domain.medicine.enums.DosageForm.TABLET, "Vien",
                AdministrationRoute.ORAL, 10, false, NOW
        );
        when(medicineRepository.findAllById(List.of(medicineId))).thenReturn(List.of(medicine));

        // Thực thi xuất đơn lẻ
        MedicalRecordExchangeExportResult exportResult = service.exportSingleRecord(RECORD_ID_1);

        assertNotNull(exportResult);
        assertEquals(1, exportResult.recordCount());
        assertEquals("application/json", exportResult.contentType());
        assertEquals("emr-exchange-BN-2026-0001-VS-2026-0001.json", exportResult.fileName());

        // Parse JSON kiểm tra tính hợp lệ cấu trúc (TC-03)
        JsonNode root = objectMapper.readTree(exportResult.content());
        assertEquals("1.0", root.get("exchangeVersion").asText());
        assertEquals("Phong kham Da khoa Tieu chuan", root.get("facility").get("clinicName").asText());
        assertEquals("BN-2026-0001", root.get("patient").get("patientCode").asText());
        assertEquals("VS-2026-0001", root.get("encounter").get("visitCode").asText());

        // Kiểm tra Chẩn đoán gắn mã ICD-10 (QTN-22)
        JsonNode diagnosesNode = root.get("diagnoses");
        assertEquals(2, diagnosesNode.size());
        assertEquals("G43", diagnosesNode.get(0).get("diagnosisCode").asText());

        // Kiểm tra Chỉ định
        JsonNode ordersNode = root.get("clinicalOrders");
        assertEquals(1, ordersNode.size());
        assertEquals("ORD-2026-0001", ordersNode.get(0).get("orderCode").asText());

        // Kiểm tra Kết quả cận lâm sàng có đầy đủ serviceCode và serviceName (F-02 Fix)
        JsonNode resultsNode = root.get("clinicalResults");
        assertEquals(1, resultsNode.size());
        assertEquals("XN-CTM", resultsNode.get(0).get("serviceCode").asText());
        assertEquals("Tong phan tich te bao mau", resultsNode.get(0).get("serviceName").asText());
        assertEquals(142.0, resultsNode.get(0).get("numericValue").asDouble());

        // Kiểm tra Đơn thuốc có đầy đủ strength, dosage, frequency, route và medicineCode chuẩn (F-05 Fix)
        JsonNode prescriptionsNode = root.get("prescriptions");
        assertEquals(1, prescriptionsNode.size());
        JsonNode medNode = prescriptionsNode.get(0).get("medications").get(0);
        assertEquals("MED-001", medNode.get("medicineCode").asText());
        assertEquals("Paracetamol 500mg", medNode.get("medicineName").asText());
        assertEquals("500mg", medNode.get("strength").asText());
        assertEquals("1 vien", medNode.get("dosage").asText());
        assertEquals(2, medNode.get("frequency").asInt());
        assertEquals("ORAL", medNode.get("route").asText());

        // Kiểm tra TC-04: Lưu Audit Log và Access Log (F-01, F-06 Fix)
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog savedAudit = auditCaptor.getValue();
        assertEquals(ActionType.EXPORT, savedAudit.getActionType());
        assertEquals(ResourceType.MEDICAL_RECORD, savedAudit.getResourceType());
        assertEquals(RECORD_ID_1, savedAudit.getResourceId());
        assertTrue(savedAudit.getDetail().contains("\"recordCount\":1"));

        verify(accessAuditService).recordRecordAccess(
                eq(PATIENT_ID),
                eq(VISIT_ID_1),
                eq(RECORD_ID_1),
                eq(ACTOR_ID),
                eq(MedicalRecordAccessAction.EXPORT),
                any(),
                eq(NOW)
        );
    }

    @Test
    @DisplayName("TC-01 & TC-04: Xuất batch bundle nhiều hồ sơ thành công")
    void testExportMultipleRecords_Success_BatchBundle() throws Exception {
        stubSecurityDefaults();

        MedicalRecord record1 = createSignedRecord(RECORD_ID_1, VISIT_ID_1);
        MedicalRecord record2 = createSignedRecord(RECORD_ID_2, VISIT_ID_2);
        when(medicalRecordRepository.findById(RECORD_ID_1)).thenReturn(Optional.of(record1));
        when(medicalRecordRepository.findById(RECORD_ID_2)).thenReturn(Optional.of(record2));

        List<MedicalRecordDiagnosis> diagnoses = List.of(
                MedicalRecordDiagnosis.create(RECORD_ID_1, UUID.randomUUID(), "G43", "Migraine", DiagnosisType.PRIMARY, "Chinh", DOCTOR_ID, NOW)
        );
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_1)).thenReturn(diagnoses);
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_2)).thenReturn(diagnoses);

        Visit visit1 = createVisit(VISIT_ID_1, "VS-2026-0001");
        Visit visit2 = createVisit(VISIT_ID_2, "VS-2026-0002");
        when(visitRepository.findById(VISIT_ID_1)).thenReturn(Optional.of(visit1));
        when(visitRepository.findById(VISIT_ID_2)).thenReturn(Optional.of(visit2));

        Patient patient = createPatient();
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

        User doctor = createDoctor();
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

        when(clinicalOrderRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(clinicalResultRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(prescriptionRepository.findByMedicalRecordId(any())).thenReturn(List.of());

        ExportMedicalRecordExchangeCommand command = ExportMedicalRecordExchangeCommand.forRecords(List.of(RECORD_ID_1, RECORD_ID_2));
        MedicalRecordExchangeExportResult result = service.exportRecords(command);

        assertEquals(2, result.recordCount());
        assertTrue(result.fileName().startsWith("emr-exchange-bundle-"));

        JsonNode root = objectMapper.readTree(result.content());
        assertEquals("1.0", root.get("exchangeVersion").asText());
        assertEquals(2, root.get("totalRecords").asInt());
        assertEquals(2, root.get("records").size());

        verify(auditLogRepository).save(any(AuditLog.class));
        verify(accessAuditService, times(2)).recordRecordAccess(
                eq(PATIENT_ID), any(), any(), eq(ACTOR_ID), eq(MedicalRecordAccessAction.EXPORT), any(), eq(NOW)
        );
    }

    @Test
    @DisplayName("F-04 Fix: Hợp nhất cả medicalRecordIds và visitIds không làm rơi rớt ID")
    void testExportRecords_MergesBothIdsWithoutDropping() {
        stubSecurityDefaults();

        MedicalRecord record1 = createSignedRecord(RECORD_ID_1, VISIT_ID_1);
        MedicalRecord record2 = createSignedRecord(RECORD_ID_2, VISIT_ID_2);
        when(medicalRecordRepository.findById(RECORD_ID_1)).thenReturn(Optional.of(record1));
        when(visitRepository.findById(VISIT_ID_2)).thenReturn(Optional.of(createVisit(VISIT_ID_2, "VS-002")));
        when(medicalRecordRepository.findByVisitId(VISIT_ID_2)).thenReturn(Optional.of(record2));

        List<MedicalRecordDiagnosis> diagnoses = List.of(
                MedicalRecordDiagnosis.create(RECORD_ID_1, UUID.randomUUID(), "G43", "Migraine", DiagnosisType.PRIMARY, "Chinh", DOCTOR_ID, NOW)
        );
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_1)).thenReturn(diagnoses);
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_2)).thenReturn(diagnoses);

        when(visitRepository.findById(VISIT_ID_1)).thenReturn(Optional.of(createVisit(VISIT_ID_1, "VS-001")));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(createPatient()));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(createDoctor()));
        when(clinicalOrderRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(clinicalResultRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(prescriptionRepository.findByMedicalRecordId(any())).thenReturn(List.of());

        ExportMedicalRecordExchangeCommand command = new ExportMedicalRecordExchangeCommand(
                List.of(RECORD_ID_1),
                List.of(VISIT_ID_2),
                "JSON"
        );

        MedicalRecordExchangeExportResult result = service.exportRecords(command);
        assertEquals(2, result.recordCount());
    }

    @Test
    @DisplayName("TC-02 & QTN-41: Từ chối xuất khi hồ sơ chưa ở trạng thái đã ký")
    void testExport_Rejected_WhenRecordNotSigned_RuleQTN41() {
        when(currentUserPort.hasPermission("MEDICAL_RECORD_EXPORT")).thenReturn(true);

        MedicalRecord draftRecord = createDraftRecord(RECORD_ID_1, VISIT_ID_1);
        when(medicalRecordRepository.findById(RECORD_ID_1)).thenReturn(Optional.of(draftRecord));

        assertThrows(MedicalRecordNotSignedException.class, () ->
                service.exportSingleRecord(RECORD_ID_1)
        );

        verify(auditLogRepository, never()).save(any());
        verify(accessAuditService, never()).recordRecordAccess(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("QTN-22 & F-05 Fix: Từ chối xuất và ném MedicalRecordMissingDiagnosisException khi thiếu mã bệnh ICD-10")
    void testExport_Rejected_WhenMissingPrimaryDiagnosisCode_RuleQTN22() {
        when(currentUserPort.hasPermission("MEDICAL_RECORD_EXPORT")).thenReturn(true);

        MedicalRecord record = createSignedRecord(RECORD_ID_1, VISIT_ID_1);
        when(medicalRecordRepository.findById(RECORD_ID_1)).thenReturn(Optional.of(record));

        // Chẩn đoán không có mã code
        List<MedicalRecordDiagnosis> diagnosesWithoutCode = List.of(
                MedicalRecordDiagnosis.create(RECORD_ID_1, null, "", "Chua xac dinh", DiagnosisType.PRIMARY, "Ghi chu", DOCTOR_ID, NOW)
        );
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_1)).thenReturn(diagnosesWithoutCode);

        assertThrows(MedicalRecordMissingDiagnosisException.class, () ->
                service.exportSingleRecord(RECORD_ID_1)
        );

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Boundary: Từ chối xuất khi số lượng hồ sơ vượt quá giới hạn 100")
    void testExport_Rejected_WhenBatchSizeExceedsMax() {
        when(currentUserPort.hasPermission("MEDICAL_RECORD_EXPORT")).thenReturn(true);

        List<UUID> excessiveIds = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            UUID id = UUID.randomUUID();
            excessiveIds.add(id);
            when(medicalRecordRepository.findById(id)).thenReturn(Optional.of(createSignedRecord(id, VISIT_ID_1)));
        }

        ExportMedicalRecordExchangeCommand command = ExportMedicalRecordExchangeCommand.forRecords(excessiveIds);

        assertThrows(ValidationException.class, () ->
                service.exportRecords(command)
        );

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("F-07 Fix: Cho phép xuất khi có permission MEDICAL_RECORD_EXPORT")
    void testExport_Allowed_WhenHasPermission() {
        when(currentUserPort.hasPermission("MEDICAL_RECORD_EXPORT")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);

        MedicalRecord record = createSignedRecord(RECORD_ID_1, VISIT_ID_1);
        when(medicalRecordRepository.findById(RECORD_ID_1)).thenReturn(Optional.of(record));

        List<MedicalRecordDiagnosis> diagnoses = List.of(
                MedicalRecordDiagnosis.create(RECORD_ID_1, UUID.randomUUID(), "G43", "Migraine", DiagnosisType.PRIMARY, "Chinh", DOCTOR_ID, NOW)
        );
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_1)).thenReturn(diagnoses);

        Visit visit = createVisit(VISIT_ID_1, "VS-2026-0001");
        when(visitRepository.findById(VISIT_ID_1)).thenReturn(Optional.of(visit));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(createPatient()));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(createDoctor()));
        when(clinicalOrderRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(clinicalResultRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(prescriptionRepository.findByMedicalRecordId(any())).thenReturn(List.of());

        MedicalRecordExchangeExportResult result = service.exportSingleRecord(RECORD_ID_1);
        assertNotNull(result);
        assertEquals(1, result.recordCount());
    }

    @Test
    @DisplayName("F-07 Fix: Cho phép xuất khi user có role ADMIN dù chưa gán permission MEDICAL_RECORD_EXPORT")
    void testExport_Allowed_WhenHasRoleAdmin() {
        when(currentUserPort.hasPermission("MEDICAL_RECORD_EXPORT")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);

        MedicalRecord record = createSignedRecord(RECORD_ID_1, VISIT_ID_1);
        when(medicalRecordRepository.findById(RECORD_ID_1)).thenReturn(Optional.of(record));

        List<MedicalRecordDiagnosis> diagnoses = List.of(
                MedicalRecordDiagnosis.create(RECORD_ID_1, UUID.randomUUID(), "G43", "Migraine", DiagnosisType.PRIMARY, "Chinh", DOCTOR_ID, NOW)
        );
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_1)).thenReturn(diagnoses);

        Visit visit = createVisit(VISIT_ID_1, "VS-2026-0001");
        when(visitRepository.findById(VISIT_ID_1)).thenReturn(Optional.of(visit));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(createPatient()));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(createDoctor()));
        when(clinicalOrderRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(clinicalResultRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(prescriptionRepository.findByMedicalRecordId(any())).thenReturn(List.of());

        MedicalRecordExchangeExportResult result = service.exportSingleRecord(RECORD_ID_1);
        assertNotNull(result);
        assertEquals(1, result.recordCount());
    }

    @Test
    @DisplayName("Security: Từ chối xuất và ném AccessDeniedException khi không có quyền lẫn vai trò")
    void testExport_Rejected_WhenNoPermissionAndNoRole() {
        when(currentUserPort.hasPermission("MEDICAL_RECORD_EXPORT")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("MANAGER")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () ->
                service.exportSingleRecord(RECORD_ID_1)
        );

        verify(medicalRecordRepository, never()).findById(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("F-08: Batch export luôn luôn trả về Bundle định dạng chuẩn ngay cả khi chỉ có 1 hồ sơ")
    void testExportRecords_BatchEndpointAlwaysReturnsBundle_EvenWithSingleRecord() throws Exception {
        stubSecurityDefaults();

        MedicalRecord record = createSignedRecord(RECORD_ID_1, VISIT_ID_1);
        when(medicalRecordRepository.findById(RECORD_ID_1)).thenReturn(Optional.of(record));

        List<MedicalRecordDiagnosis> diagnoses = List.of(
                MedicalRecordDiagnosis.create(RECORD_ID_1, UUID.randomUUID(), "G43", "Migraine", DiagnosisType.PRIMARY, "Chinh", DOCTOR_ID, NOW)
        );
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_1)).thenReturn(diagnoses);
        when(visitRepository.findById(VISIT_ID_1)).thenReturn(Optional.of(createVisit(VISIT_ID_1, "VS-2026-0001")));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(createPatient()));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(createDoctor()));
        when(clinicalOrderRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(clinicalResultRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(prescriptionRepository.findByMedicalRecordId(any())).thenReturn(List.of());

        ExportMedicalRecordExchangeCommand command = ExportMedicalRecordExchangeCommand.forRecords(List.of(RECORD_ID_1));
        MedicalRecordExchangeExportResult result = service.exportRecords(command);

        assertEquals(1, result.recordCount());
        assertTrue(result.fileName().startsWith("emr-exchange-bundle-"));

        JsonNode root = objectMapper.readTree(result.content());
        assertEquals("1.0", root.get("exchangeVersion").asText());
        assertEquals(1, root.get("totalRecords").asInt());
        assertNotNull(root.get("bundleId"));
        assertEquals(1, root.get("records").size());
        assertEquals("VS-2026-0001", root.get("records").get(0).get("encounter").get("visitCode").asText());
    }

    @Test
    @DisplayName("F-03: Xử lý an toàn khi bác sĩ khám không tìm thấy trong users, fallback sang record.signedBy mà không bị lỗi")
    void testExportSingleRecord_WhenDoctorIdNull_FallbackToSignedBy() throws Exception {
        stubSecurityDefaults();

        MedicalRecord record = createSignedRecord(RECORD_ID_1, VISIT_ID_1);
        when(medicalRecordRepository.findById(RECORD_ID_1)).thenReturn(Optional.of(record));

        List<MedicalRecordDiagnosis> diagnoses = List.of(
                MedicalRecordDiagnosis.create(RECORD_ID_1, UUID.randomUUID(), "G43", "Migraine", DiagnosisType.PRIMARY, "Chinh", DOCTOR_ID, NOW)
        );
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_1)).thenReturn(diagnoses);

        UUID otherDoctorId = UUID.randomUUID();
        Visit visitWithOtherDoctor = Visit.restore(
                VISIT_ID_1, "VS-2026-0001", PATIENT_ID, otherDoctorId, null, null,
                VisitType.WALK_IN, VisitStatus.COMPLETED,
                NOW.minusSeconds(3600), NOW.minusSeconds(1800), NOW.minusSeconds(600),
                "Kham", null, DOCTOR_ID, NOW.minusSeconds(3600), NOW.minusSeconds(600)
        );
        when(visitRepository.findById(VISIT_ID_1)).thenReturn(Optional.of(visitWithOtherDoctor));
        when(userRepository.findById(otherDoctorId)).thenReturn(Optional.empty()); // Bác sĩ không tìm thấy trong users
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(createPatient()));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(createDoctor())); // Bác sĩ ký
        when(clinicalOrderRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(clinicalResultRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(prescriptionRepository.findByMedicalRecordId(any())).thenReturn(List.of());

        MedicalRecordExchangeExportResult result = service.exportSingleRecord(RECORD_ID_1);
        assertNotNull(result);

        JsonNode root = objectMapper.readTree(result.content());
        assertEquals("BS. Tran Van B", root.get("encounter").get("doctorName").asText());
    }

    @Test
    @DisplayName("F-04: Ẩn danh toàn diện các trường nhạy cảm PII khi anonymizationModeState được bật")
    void testExportRecord_WhenAnonymizationEnabled_MasksAllSensitiveFields() throws Exception {
        stubSecurityDefaults();
        anonymizationModeState.setEnabled(true);

        MedicalRecord record = createSignedRecord(RECORD_ID_1, VISIT_ID_1);
        when(medicalRecordRepository.findById(RECORD_ID_1)).thenReturn(Optional.of(record));

        List<MedicalRecordDiagnosis> diagnoses = List.of(
                MedicalRecordDiagnosis.create(RECORD_ID_1, UUID.randomUUID(), "G43", "Migraine", DiagnosisType.PRIMARY, "Chinh", DOCTOR_ID, NOW)
        );
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(RECORD_ID_1)).thenReturn(diagnoses);
        when(visitRepository.findById(VISIT_ID_1)).thenReturn(Optional.of(createVisit(VISIT_ID_1, "VS-2026-0001")));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(createPatient()));
        when(userRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(createDoctor()));
        when(clinicalOrderRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(clinicalResultRepository.findByVisitId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(prescriptionRepository.findByMedicalRecordId(any())).thenReturn(List.of());

        MedicalRecordExchangeExportResult result = service.exportSingleRecord(RECORD_ID_1);
        JsonNode root = objectMapper.readTree(result.content());
        JsonNode patientNode = root.get("patient");

        assertTrue(patientNode.get("fullName").asText().startsWith("BỆNH NHÂN #"));
        assertTrue(patientNode.get("phone").asText().contains("******"));
        assertEquals("[ĐỊA CHỈ ĐÃ ẨN DANH]", patientNode.get("address").asText());
        assertTrue(patientNode.get("identityNumber").isNull());
        assertTrue(patientNode.get("insuranceNumber").isNull());
    }

    @Test
    @DisplayName("F-07: Khởi tạo Command với format không hỗ trợ ném ValidationException")
    void testExportCommand_ThrowsValidationException_WhenUnsupportedFormat() {
        assertThrows(ValidationException.class, () ->
                new ExportMedicalRecordExchangeCommand(List.of(RECORD_ID_1), null, "XML")
        );
        assertThrows(ValidationException.class, () ->
                new ExportMedicalRecordExchangeCommand(List.of(RECORD_ID_1), null, "PDF")
        );
    }
}

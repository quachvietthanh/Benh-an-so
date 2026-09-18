package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordUnauthorizedSignerException;
import com.benhsoan.domain.medicalrecord.exception.PendingClinicalOrdersWarningException;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderEntity;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderItemEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordDiagnosisEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalOrderItemRepository;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalOrderRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAccessLogRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordDiagnosisRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;
import com.benhsoan.port.dto.command.medicalrecord.SignMedicalRecordCommand;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:sign-audit-it;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("SignMedicalRecordAuditIntegrationTest - Kiểm thử transaction rollback không làm mất audit log khi cảnh báo chỉ định treo")
class SignMedicalRecordAuditIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:00:00Z");

    @Autowired private SignMedicalRecordService signMedicalRecordService;
    @Autowired private JpaVisitRepository visitRepository;
    @Autowired private JpaMedicalRecordRepository medicalRecordRepository;
    @Autowired private JpaMedicalRecordDiagnosisRepository diagnosisRepository;
    @Autowired private JpaClinicalOrderRepository clinicalOrderRepository;
    @Autowired private JpaClinicalOrderItemRepository clinicalOrderItemRepository;
    @Autowired private JpaMedicalRecordAccessLogRepository accessLogRepository;

    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    private UUID doctorId;
    private UUID patientId;
    private UUID visitId;
    private UUID recordId;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        visitId = UUID.randomUUID();
        recordId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        // Seed visit
        visitRepository.saveAndFlush(VisitEntity.builder()
                .id(visitId)
                .visitCode("VIS-IT-001")
                .patientId(patientId)
                .doctorId(doctorId)
                .createdBy(doctorId)
                .reason("Khám tổng quát")
                .visitType(VisitType.WALK_IN)
                .status(VisitStatus.IN_PROGRESS)
                .visitAt(NOW)
                .startedAt(NOW)
                .createdAt(NOW)
                .build());

        // Seed medical record
        medicalRecordRepository.saveAndFlush(MedicalRecordEntity.builder()
                .id(recordId)
                .visitId(visitId)
                .status(MedicalRecordStatus.OPEN)
                .chiefComplaint("Sốt cao liên tục")
                .clinicalProgress("Bệnh nhân tỉnh, sốt 39 độ")
                .physicalExamination("Họng đỏ nhẹ")
                .conclusion("Theo dõi sốt siêu vi")
                .treatmentPlan("Uống hạ sốt, bù điện giải")
                .doctorInstructions("Tái khám sau 2 ngày nếu không đỡ")
                .createdBy(doctorId)
                .createdAt(NOW)
                .build());

        // Seed diagnosis
        diagnosisRepository.saveAndFlush(MedicalRecordDiagnosisEntity.builder()
                .id(UUID.randomUUID())
                .medicalRecordId(recordId)
                .diagnosisCode("J00")
                .diagnosisName("Viêm mũi họng cấp")
                .diagnosisType(DiagnosisType.PRIMARY)
                .diagnosedBy(doctorId)
                .diagnosedAt(NOW)
                .createdAt(NOW)
                .build());
    }

    @Test
    @DisplayName("P0-01 Verified: Ký bệnh án khi còn chỉ định treo -> Ném ngoại lệ 409 VÀ Audit log vẫn được lưu an toàn vào CSDL (không bị rollback)")
    void auditLogPersistedWhenSignatureBlockedByPendingOrders() {
        // Seed clinical order with pending item
        UUID orderId = UUID.randomUUID();
        clinicalOrderRepository.saveAndFlush(ClinicalOrderEntity.builder()
                .id(orderId)
                .orderCode("ORD-IT-001")
                .visitId(visitId)
                .medicalRecordId(recordId)
                .patientId(patientId)
                .orderedBy(doctorId)
                .clinicalReason("Kiểm tra chỉ số viêm")
                .status(ClinicalOrderStatus.ORDERED)
                .orderedAt(NOW)
                .createdAt(NOW)
                .build());

        clinicalOrderItemRepository.saveAndFlush(ClinicalOrderItemEntity.builder()
                .id(UUID.randomUUID())
                .clinicalOrderId(orderId)
                .clinicalServiceId(UUID.randomUUID())
                .serviceCode("CBC")
                .serviceName("Tổng phân tích tế bào máu")
                .status(ClinicalOrderItemStatus.PENDING)
                .createdAt(NOW)
                .build());

        // When: Doctor attempts to sign without acknowledgement
        PendingClinicalOrdersWarningException ex = assertThrows(
                PendingClinicalOrdersWarningException.class,
                () -> signMedicalRecordService.sign(recordId, new SignMedicalRecordCommand("SIG_DATA", false))
        );

        assertNotNull(ex);
        assertEquals(1, ex.getPendingServices().size());
        assertEquals("Tổng phân tích tế bào máu", ex.getPendingServices().getFirst());

        // Then: Verify that audit log row WAS ACTUALLY SAVED in database!
        List<MedicalRecordAccessLogEntity> logs = accessLogRepository.findAll();
        boolean foundBlockedAudit = logs.stream().anyMatch(log ->
                recordId.equals(log.getMedicalRecordId())
                        && log.getAction() == MedicalRecordAccessAction.SIGN
                        && log.getDetail() != null
                        && log.getDetail().contains("Signature blocked: Pending paraclinical orders waiting for results: Tổng phân tích tế bào máu")
        );

        assertTrue(foundBlockedAudit, "Audit log cảnh báo ký bệnh án phải tồn tại trong CSDL sau khi transaction chính bị rollback!");
    }

    @Test
    @DisplayName("Ký bệnh án thành công khi bác sĩ xác nhận chỉ định treo (acknowledgePendingOrders = true)")
    void signSucceedsWhenPendingOrdersAcknowledged() {
        // Seed clinical order with pending item
        UUID orderId = UUID.randomUUID();
        clinicalOrderRepository.saveAndFlush(ClinicalOrderEntity.builder()
                .id(orderId)
                .orderCode("ORD-IT-002")
                .visitId(visitId)
                .medicalRecordId(recordId)
                .patientId(patientId)
                .orderedBy(doctorId)
                .clinicalReason("Kiểm tra chỉ số viêm")
                .status(ClinicalOrderStatus.ORDERED)
                .orderedAt(NOW)
                .createdAt(NOW)
                .build());

        clinicalOrderItemRepository.saveAndFlush(ClinicalOrderItemEntity.builder()
                .id(UUID.randomUUID())
                .clinicalOrderId(orderId)
                .clinicalServiceId(UUID.randomUUID())
                .serviceCode("CRP")
                .serviceName("Định lượng CRP")
                .status(ClinicalOrderItemStatus.PENDING)
                .createdAt(NOW)
                .build());

        // When: Doctor signs with acknowledgement
        var result = signMedicalRecordService.sign(recordId, new SignMedicalRecordCommand("SIG_DATA", true));

        // Then:
        assertEquals(MedicalRecordStatus.SIGNED.name(), result.status().name());

        List<MedicalRecordAccessLogEntity> logs = accessLogRepository.findAll();
        boolean foundSignedAudit = logs.stream().anyMatch(log ->
                recordId.equals(log.getMedicalRecordId())
                        && log.getAction() == MedicalRecordAccessAction.SIGN
                        && log.getDetail() != null
                        && log.getDetail().contains("acknowledged pending paraclinical orders")
        );

        assertTrue(foundSignedAudit, "Audit log ký thành công với xác nhận chỉ định treo phải tồn tại trong CSDL!");
    }

    @Test
    @DisplayName("P0-1 Verified: Bác sĩ không phụ trách ký bệnh án -> Ném ngoại lệ VÀ Audit log SIGN (Rejected) vẫn được commit độc lập vào CSDL")
    void auditLogPersistedWhenSignatureRejectedForUnauthorizedDoctor() {
        UUID otherDoctorId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(otherDoctorId);

        assertThrows(
                MedicalRecordUnauthorizedSignerException.class,
                () -> signMedicalRecordService.sign(recordId, new SignMedicalRecordCommand("SIG_DATA", false))
        );

        List<MedicalRecordAccessLogEntity> logs = accessLogRepository.findAll();
        boolean foundRejectedAudit = logs.stream().anyMatch(log ->
                recordId.equals(log.getMedicalRecordId())
                        && log.getAction() == MedicalRecordAccessAction.SIGN
                        && log.getDetail() != null
                        && log.getDetail().contains("Signature rejected: User is not doctor in charge")
                        && otherDoctorId.equals(log.getAccessedBy())
        );

        assertTrue(foundRejectedAudit, "Audit log từ chối ký phải tồn tại trong CSDL sau khi transaction chính bị rollback!");
    }
}

package com.benhsoan.application.ucservice.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.benhsoan.application.ucservice.medicalrecord.SignMedicalRecordService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordUnauthorizedSignerException;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitEncounterAccessDeniedException;
import com.benhsoan.persistence.entity.auditlog.AuditLogEntity;
import com.benhsoan.persistence.entity.auth.RoleEntity;
import com.benhsoan.persistence.entity.auth.UserEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordDiagnosisEntity;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.entity.patient.PatientEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.entity.visit.VisitHandoverEntity;
import com.benhsoan.persistence.jpaRepository.auditlog.JpaAuditLogRepository;
import com.benhsoan.persistence.jpaRepository.auth.JpaRoleRepository;
import com.benhsoan.persistence.jpaRepository.auth.JpaUserRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordAccessLogRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordDiagnosisRepository;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordRepository;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitHandoverRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;
import com.benhsoan.port.dto.command.medicalrecord.SignMedicalRecordCommand;
import com.benhsoan.port.dto.command.visit.HandoverPatientCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.dto.result.VisitHandoverResult;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:patient-handover-it;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("NCL-04-CN-014: Integration Tests - Bàn giao bệnh nhân sang bác sĩ khác")
class PatientHandoverIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    @Autowired private HandoverPatientService handoverPatientService;
    @Autowired private GetVisitHandoversService getVisitHandoversService;
    @Autowired private SignMedicalRecordService signMedicalRecordService;

    @Autowired private JpaVisitRepository visitRepository;
    @Autowired private JpaVisitHandoverRepository visitHandoverRepository;
    @Autowired private JpaMedicalRecordRepository medicalRecordRepository;
    @Autowired private JpaMedicalRecordDiagnosisRepository diagnosisRepository;
    @Autowired private JpaMedicalRecordAccessLogRepository accessLogRepository;
    @Autowired private JpaUserRepository userRepository;
    @Autowired private JpaRoleRepository roleRepository;
    @Autowired private JpaPatientRepository patientRepository;
    @Autowired private JpaAuditLogRepository auditLogRepository;

    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;

    private UUID doctorAId;
    private UUID doctorBId;
    private UUID patientId;
    private UUID visitId;
    private UUID recordId;

    @BeforeEach
    void setUp() {
        // Clean dependent data
        visitHandoverRepository.deleteAll();
        diagnosisRepository.deleteAll();
        medicalRecordRepository.deleteAll();
        visitRepository.deleteAll();
        accessLogRepository.deleteAll();
        auditLogRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();

        doctorAId = UUID.randomUUID();
        doctorBId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        visitId = UUID.randomUUID();
        recordId = UUID.randomUUID();

        when(clockPort.now()).thenReturn(NOW);

        if (roleRepository.findById(RoleConstants.DOCTOR).isEmpty()) {
            roleRepository.saveAndFlush(RoleEntity.builder()
                    .id(RoleConstants.DOCTOR)
                    .name("DOCTOR")
                    .description("Bác sĩ điều trị")
                    .isSystem(true)
                    .createdAt(NOW)
                    .updatedAt(NOW)
                    .build());
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // Seed Doctor A
        userRepository.saveAndFlush(UserEntity.builder()
                .id(doctorAId)
                .username("doctorA_" + suffix)
                .passwordHash("hashed")
                .fullName("Bác sĩ Nguyễn Văn A")
                .email("doctorA_" + suffix + "@hospital.vn")
                .phone("090" + suffix.substring(0, 6))
                .roleId(RoleConstants.DOCTOR)
                .active(true)
                .createdAt(NOW)
                .build());

        // Seed Doctor B
        userRepository.saveAndFlush(UserEntity.builder()
                .id(doctorBId)
                .username("doctorB_" + suffix)
                .passwordHash("hashed")
                .fullName("Bác sĩ Trần Thị B")
                .email("doctorB_" + suffix + "@hospital.vn")
                .phone("091" + suffix.substring(0, 6))
                .roleId(RoleConstants.DOCTOR)
                .active(true)
                .createdAt(NOW)
                .build());

        // Seed Patient
        patientRepository.saveAndFlush(PatientEntity.builder()
                .id(patientId)
                .patientCode("BN" + suffix)
                .fullName("Bệnh nhân Lê Văn C")
                .phone("092" + suffix.substring(0, 6))
                .dateOfBirth(java.time.LocalDate.of(1990, 5, 20))
                .gender(com.benhsoan.domain.patient.enums.Gender.MALE)
                .active(true)
                .createdBy(doctorAId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build());

        // Seed Visit (đang khám bởi Doctor A)
        visitRepository.saveAndFlush(VisitEntity.builder()
                .id(visitId)
                .visitCode("VIS" + suffix)
                .patientId(patientId)
                .doctorId(doctorAId)
                .createdBy(doctorAId)
                .reason("Khám thần kinh")
                .visitType(VisitType.WALK_IN)
                .status(VisitStatus.IN_PROGRESS)
                .visitAt(NOW)
                .startedAt(NOW)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build());

        // Seed Medical Record (bản thảo)
        medicalRecordRepository.saveAndFlush(MedicalRecordEntity.builder()
                .id(recordId)
                .visitId(visitId)
                .status(MedicalRecordStatus.OPEN)
                .chiefComplaint("Đau đầu chóng mặt")
                .symptoms("Đau nửa đầu")
                .medicalHistory("Không tiền sử bệnh")
                .physicalExamination("Huyết áp bình thường")
                .clinicalProgress("Theo dõi thêm")
                .treatmentPlan("Chờ kết quả hội chẩn")
                .doctorInstructions("Nghỉ ngơi")
                .conclusion("Nghi ngờ đau nửa đầu")
                .createdBy(doctorAId)
                .createdAt(NOW)
                .build());

        // Seed Diagnosis
        diagnosisRepository.saveAndFlush(MedicalRecordDiagnosisEntity.builder()
                .id(UUID.randomUUID())
                .medicalRecordId(recordId)
                .diagnosisCode("G43")
                .diagnosisName("Migraine")
                .diagnosisType(DiagnosisType.PRIMARY)
                .diagnosedBy(doctorAId)
                .diagnosedAt(NOW)
                .createdAt(NOW)
                .build());
    }

    @Test
    @DisplayName("TC-01: Bàn giao bệnh nhân thành công sang bác sĩ khác -> cập nhật quyền cho bác sĩ mới và lưu vết")
    void tc01_handoverPatientSuccess_targetDoctorReceivesEncounterAndCanViewHistory() {
        // Given: Doctor A đang đăng nhập
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        HandoverPatientCommand command = new HandoverPatientCommand(doctorBId, "Cần ý kiến chuyên khoa sâu");

        // When: Doctor A bàn giao ca khám cho Doctor B
        VisitHandoverResult result = handoverPatientService.handover(visitId, command);

        // Then: Kết quả trả về chính xác
        assertNotNull(result);
        assertEquals(doctorAId, result.fromDoctorId());
        assertEquals("Bác sĩ Nguyễn Văn A", result.fromDoctorName());
        assertEquals(doctorBId, result.toDoctorId());
        assertEquals("Bác sĩ Trần Thị B", result.toDoctorName());
        assertEquals("Cần ý kiến chuyên khoa sâu", result.reason());

        // Verify DB: Visit được cập nhật doctorId = Doctor B và initialDoctorId = Doctor A
        VisitEntity updatedVisit = visitRepository.findById(visitId).orElseThrow();
        assertEquals(doctorBId, updatedVisit.getDoctorId());
        assertEquals(doctorAId, updatedVisit.getInitialDoctorId());

        // Verify DB: Lịch sử bàn giao visit_handovers được lưu đầy đủ
        List<VisitHandoverEntity> handovers = visitHandoverRepository.findByVisitIdOrderByHandedOverAtAsc(visitId);
        assertEquals(1, handovers.size());
        assertEquals(doctorAId, handovers.get(0).getFromDoctorId());
        assertEquals(doctorBId, handovers.get(0).getToDoctorId());
        assertEquals("Cần ý kiến chuyên khoa sâu", handovers.get(0).getReason());

        // When: Doctor B đăng nhập và xem lịch sử bàn giao
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorBId);
        List<VisitHandoverResult> history = getVisitHandoversService.getHandovers(visitId);

        assertEquals(1, history.size());
        assertEquals("Bác sĩ Nguyễn Văn A", history.get(0).fromDoctorName());
        assertEquals("Bác sĩ Trần Thị B", history.get(0).toDoctorName());
    }

    @Test
    @DisplayName("TC-02: Bàn giao thất bại khi hồ sơ bệnh án đã ký/khóa -> ném MedicalRecordAlreadyLockedException")
    void tc02_handoverFails_whenMedicalRecordAlreadyLockedOrSigned() {
        // Given: Bệnh án đã được ký bởi Doctor A
        MedicalRecordEntity record = medicalRecordRepository.findById(recordId).orElseThrow();
        record.setStatus(MedicalRecordStatus.SIGNED);
        record.setSignedBy(doctorAId);
        record.setSignedAt(NOW);
        medicalRecordRepository.saveAndFlush(record);

        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);

        HandoverPatientCommand command = new HandoverPatientCommand(doctorBId, "Bàn giao ca bệnh đã kết thúc");

        // When / Then: Báo lỗi bệnh án đã khóa
        assertThrows(MedicalRecordAlreadyLockedException.class, () ->
                handoverPatientService.handover(visitId, command)
        );

        // Verify DB: Visit vẫn thuộc Doctor A, không tạo bản ghi handover nào
        VisitEntity visit = visitRepository.findById(visitId).orElseThrow();
        assertEquals(doctorAId, visit.getDoctorId());
        assertEquals(0, visitHandoverRepository.findByVisitIdOrderByHandedOverAtAsc(visitId).size());
    }

    @Test
    @DisplayName("TC-03: Bác sĩ chuyển đi (Doctor A) bị từ chối quyền can thiệp vào bệnh án sau khi bàn giao")
    void tc03_initialDoctorLostEditPermissionAfterHandover() {
        // Given: Đã bàn giao sang Doctor B
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        handoverPatientService.handover(visitId, new HandoverPatientCommand(doctorBId, "Chuyển giao"));

        // When: Doctor A cố gắng ký bệnh án
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        assertThrows(MedicalRecordUnauthorizedSignerException.class, () ->
                signMedicalRecordService.sign(recordId, new SignMedicalRecordCommand("DR_A_SIG"))
        );

        // Doctor A cố gắng bàn giao lại lượt khám này
        assertThrows(VisitEncounterAccessDeniedException.class, () ->
                handoverPatientService.handover(visitId, new HandoverPatientCommand(UUID.randomUUID(), "Lại bàn giao"))
        );

        // Verify audit log from P0-1: Rejected sign attempt is logged
        List<MedicalRecordAccessLogEntity> accessLogs = accessLogRepository.findAll();
        boolean hasRejectedSignAudit = accessLogs.stream()
                .anyMatch(log -> log.getAction() == MedicalRecordAccessAction.SIGN
                        && log.getAccessedBy().equals(doctorAId)
                        && log.getDetail().contains("Signature rejected: User is not doctor in charge"));
        assertEquals(true, hasRejectedSignAudit);
    }

    @Test
    @DisplayName("TC-04: Bác sĩ tiếp nhận (Doctor B) ký bệnh án thành công, ghi nhận đúng người kết thúc và bảo lưu bác sĩ ban đầu")
    void tc04_targetDoctorSignsSuccessfully_auditLogPreservesInitialDoctor() {
        // Given: Bàn giao thành công sang Doctor B
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        handoverPatientService.handover(visitId, new HandoverPatientCommand(doctorBId, "Chuyển giao bác sĩ B"));

        // When: Doctor B đăng nhập và ký bệnh án
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorBId);
        MedicalRecordResult signResult = signMedicalRecordService.sign(recordId, new SignMedicalRecordCommand("DR_B_SIG"));

        // Then: Chữ ký ghi nhận bác sĩ B
        assertEquals(MedicalRecordStatus.SIGNED, signResult.status());
        assertEquals(doctorBId, signResult.signedBy());

        // Verify Audit Log: Lưu vết ghi nhận bảo lưu bác sĩ ban đầu
        List<MedicalRecordAccessLogEntity> accessLogs = accessLogRepository.findAll();
        boolean hasSignAuditWithInitialDoctor = accessLogs.stream()
                .anyMatch(log -> log.getAction() == MedicalRecordAccessAction.SIGN
                        && log.getAccessedBy().equals(doctorBId)
                        && log.getDetail().contains("handed over from initial doctor: " + doctorAId));

        assertEquals(true, hasSignAuditWithInitialDoctor);

        // Verify: Bác sĩ ban đầu vẫn được bảo lưu trên Visit
        VisitEntity finalVisit = visitRepository.findById(visitId).orElseThrow();
        assertEquals(doctorBId, finalVisit.getDoctorId());
        assertEquals(doctorAId, finalVisit.getInitialDoctorId());
    }

    @Test
    @DisplayName("P1-1 Verified: Bác sĩ ngoài ca khám (dù có quyền MEDICAL_RECORD_HANDOVER) không thể xem lịch sử bàn giao")
    void tc05_unrelatedDoctorCannotViewHandoversEvenWithHandoverPermission() {
        // Given: Đã bàn giao sang Doctor B
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorAId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        handoverPatientService.handover(visitId, new HandoverPatientCommand(doctorBId, "Chuyển giao"));

        // Doctor C là một bác sĩ khác, có quyền MEDICAL_RECORD_HANDOVER
        UUID doctorCId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorCId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasPermission("MEDICAL_RECORD_HANDOVER")).thenReturn(true);

        // When & Then: Bị từ chối xem lịch sử
        assertThrows(VisitEncounterAccessDeniedException.class, () ->
                getVisitHandoversService.getHandovers(visitId)
        );
    }

    @Test
    @DisplayName("P3-1 Verified: Bác sĩ không phụ trách cố ý bàn giao ca khám -> Bị từ chối và ghi log an ninh ACCESS_DENIED")
    void tc06_unauthorizedHandoverAttemptLogsAccessDeniedAudit() {
        UUID doctorCId = UUID.randomUUID();
        when(currentUserPort.getCurrentUserId()).thenReturn(doctorCId);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasPermission("MEDICAL_RECORD_HANDOVER")).thenReturn(true);

        assertThrows(VisitEncounterAccessDeniedException.class, () ->
                handoverPatientService.handover(visitId, new HandoverPatientCommand(doctorBId, "Cố ý can thiệp"))
        );

        List<AuditLogEntity> auditLogs = auditLogRepository.findAll();
        boolean hasAccessDeniedAudit = auditLogs.stream()
                .anyMatch(log -> log.getActionType() == ActionType.ACCESS_DENIED
                        && log.getResourceType() == ResourceType.VISIT
                        && visitId.equals(log.getResourceId())
                        && doctorCId.equals(log.getUserId()));

        assertEquals(true, hasAccessDeniedAudit, "Audit log ACCESS_DENIED phải được lưu vào CSDL khi bàn giao trái phép!");
    }
}

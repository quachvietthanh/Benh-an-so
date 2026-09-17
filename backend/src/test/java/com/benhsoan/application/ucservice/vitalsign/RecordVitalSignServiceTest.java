package com.benhsoan.application.ucservice.vitalsign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitInvalidStatusException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.port.dto.command.vitalsign.RecordVitalSignCommand;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.vitalsign.VitalSignRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class RecordVitalSignServiceTest {

    @Mock
    private VitalSignRepository vitalSignRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private VitalSignAuthorizationService authorizationService;
    @Mock
    private MedicalRecordAccessAuditService accessAuditService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ClockPort clockPort;

    private VitalSignResultMapper resultMapper;
    private RecordVitalSignService service;

    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-20T08:00:00Z");

    @BeforeEach
    void setUp() {
        resultMapper = new VitalSignResultMapper();
        service = new RecordVitalSignService(
                vitalSignRepository,
                visitRepository,
                medicalRecordRepository,
                authorizationService,
                accessAuditService,
                auditLogRepository,
                resultMapper,
                clockPort
        );
    }

    private Visit activeVisit() {
        Visit v = Visit.create("VISIT-001", PATIENT_ID, DOCTOR_ID, null, null, VisitType.WALK_IN, NOW, "Khám tổng quát", null, DOCTOR_ID, NOW);
        v.start(NOW.plusSeconds(60));
        return v;
    }

    @Test
    @DisplayName("TC-01: Ghi nhận chỉ số sinh tồn thành công khi ca khám đang hoạt động")
    void recordsVitalSignSuccessfully() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        Visit visit = activeVisit();
        MedicalRecord record = MedicalRecord.create(
                visit.getId(), "Ly do", "Trieu chung", "Tien su", "Kham", "Dien tien", "Ke hoach", "Loi dan", "Ket luan", DOCTOR_ID, NOW
        );
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(record));
        when(clockPort.now()).thenReturn(NOW);
        when(vitalSignRepository.save(any(VitalSign.class))).thenAnswer(inv -> inv.getArgument(0));

        RecordVitalSignCommand cmd = new RecordVitalSignCommand(
                VISIT_ID,
                80,
                120,
                80,
                new BigDecimal("37.0"),
                16,
                new BigDecimal("60.0"),
                new BigDecimal("165.0"),
                98,
                "Khám bình thường"
        );

        VitalSignResult result = service.record(cmd);

        assertNotNull(result);
        assertEquals(80, result.pulse());
        assertEquals(120, result.bloodPressureSystolic());
        assertEquals(80, result.bloodPressureDiastolic());
        verify(accessAuditService).recordRecordAccess(any(), any(), any(), any(), any(), any(), any());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Precondition NCL-04-CN-007: Từ chối khi ca khám mới chỉ ở trạng thái WAITING")
    void rejectsWhenVisitIsWaiting() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        Visit visit = Visit.create("VISIT-001", PATIENT_ID, DOCTOR_ID, null, null, VisitType.WALK_IN, NOW, "Khám tổng quát", null, DOCTOR_ID, NOW);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        RecordVitalSignCommand cmd = new RecordVitalSignCommand(
                VISIT_ID, 80, 120, 80, new BigDecimal("37.0"), 16, null, null, null, null
        );

        assertThrows(VisitInvalidStatusException.class, () -> service.record(cmd));
    }

    @Test
    @DisplayName("Dependency NCL-04-CN-001: Từ chối khi bệnh án của lượt khám chưa được mở")
    void rejectsWhenMedicalRecordNotYetCreated() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        Visit visit = activeVisit();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());

        RecordVitalSignCommand cmd = new RecordVitalSignCommand(
                VISIT_ID, 80, 120, 80, new BigDecimal("37.0"), 16, null, null, null, null
        );

        assertThrows(MedicalRecordNotFoundException.class, () -> service.record(cmd));
    }

    @Test
    @DisplayName("Từ chối khi ca khám không tồn tại")
    void rejectsWhenVisitNotFound() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.empty());

        RecordVitalSignCommand cmd = new RecordVitalSignCommand(
                VISIT_ID, 80, 120, 80, new BigDecimal("37.0"), 16, null, null, null, null
        );

        assertThrows(VisitNotFoundException.class, () -> service.record(cmd));
    }

    @Test
    @DisplayName("Từ chối khi bác sĩ không phụ trách ca khám")
    void rejectsWhenDoctorNotAssigned() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        Visit visit = activeVisit();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));
        doThrow(new MedicalRecordAccessDeniedException()).when(authorizationService).requireVisitDoctorAccess(visit.getDoctorId());

        RecordVitalSignCommand cmd = new RecordVitalSignCommand(
                VISIT_ID, 80, 120, 80, new BigDecimal("37.0"), 16, null, null, null, null
        );

        assertThrows(MedicalRecordAccessDeniedException.class, () -> service.record(cmd));
    }

    @Test
    @DisplayName("QTN-07: Từ chối khi ca khám đã hoàn thành")
    void rejectsWhenVisitIsCompleted() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        Visit visit = activeVisit();
        visit.complete(NOW.plusSeconds(3600)); // status COMPLETED -> isActive() = false
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        RecordVitalSignCommand cmd = new RecordVitalSignCommand(
                VISIT_ID, 80, 120, 80, new BigDecimal("37.0"), 16, null, null, null, null
        );

        assertThrows(VisitInvalidStatusException.class, () -> service.record(cmd));
    }

    @Test
    @DisplayName("QTN-07: Từ chối khi bệnh án đã bị ký/khóa")
    void rejectsWhenMedicalRecordIsAlreadyLocked() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        Visit visit = activeVisit();
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        MedicalRecord record = MedicalRecord.create(
                visit.getId(), "Ly do", "Trieu chung", "Tien su", "Kham", "Dien tien", "Ke hoach", "Loi dan", "Ket luan", DOCTOR_ID, NOW
        );
        record.sign("SigData", DOCTOR_ID, NOW);
        record.lock(DOCTOR_ID, NOW); // locked
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(record));

        RecordVitalSignCommand cmd = new RecordVitalSignCommand(
                VISIT_ID, 80, 120, 80, new BigDecimal("37.0"), 16, null, null, null, null
        );

        assertThrows(MedicalRecordAlreadyLockedException.class, () -> service.record(cmd));
    }
}

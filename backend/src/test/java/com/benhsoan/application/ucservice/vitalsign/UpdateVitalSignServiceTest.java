package com.benhsoan.application.ucservice.vitalsign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.visit.exception.VisitInvalidStatusException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.domain.vitalsign.exception.VitalSignNotFoundException;
import com.benhsoan.port.dto.command.vitalsign.UpdateVitalSignCommand;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.vitalsign.VitalSignRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class UpdateVitalSignServiceTest {

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
    private UpdateVitalSignService service;

    private static final UUID VITAL_SIGN_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-20T08:00:00Z");

    @BeforeEach
    void setUp() {
        resultMapper = new VitalSignResultMapper();
        service = new UpdateVitalSignService(
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
    @DisplayName("Cập nhật chỉ số sinh tồn thành công và tự động gắn medicalRecordId khi trước đó null")
    void updatesSuccessfullyWhenVisitInProgressAndRecordOpen() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        when(clockPort.now()).thenReturn(NOW);

        Visit visit = activeVisit();
        MedicalRecord record = MedicalRecord.create(
                visit.getId(), "Ly do", "Trieu chung", "Tien su", "Kham", "Dien tien", "Ke hoach", "Loi dan", "Ket luan", DOCTOR_ID, NOW
        );

        // Vital sign initially has null medicalRecordId
        VitalSign vs = VitalSign.create(
                visit.getId(), PATIENT_ID, null, 80, 120, 80, new BigDecimal("37.0"), 16,
                new BigDecimal("60.0"), new BigDecimal("165.0"), 98, "Ghi chú ban đầu", DOCTOR_ID, NOW
        );

        when(vitalSignRepository.findById(VITAL_SIGN_ID)).thenReturn(Optional.of(vs));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(record));
        when(vitalSignRepository.save(any(VitalSign.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateVitalSignCommand cmd = new UpdateVitalSignCommand(
                72, 118, 78, new BigDecimal("36.8"), 16, new BigDecimal("60.0"),
                new BigDecimal("165.0"), 99, "Sau khi nghỉ ngơi"
        );

        VitalSignResult result = service.update(VITAL_SIGN_ID, cmd);

        assertNotNull(result);
        assertEquals(72, result.pulse());
        assertEquals(118, result.bloodPressureSystolic());
        assertEquals(78, result.bloodPressureDiastolic());
        assertEquals("Sau khi nghỉ ngơi", result.note());
        assertEquals(record.getId(), vs.getMedicalRecordId());

        verify(accessAuditService).recordRecordAccess(
                eq(PATIENT_ID), eq(visit.getId()), eq(record.getId()), eq(DOCTOR_ID),
                eq(MedicalRecordAccessAction.UPDATE), any(), eq(NOW)
        );
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("QTN-07: Chặn cập nhật khi bệnh án đã bị khóa dù medicalRecordId của vitalSign ban đầu là null")
    void rejectsUpdateWhenMedicalRecordIsAlreadyLocked() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);

        Visit visit = activeVisit();
        MedicalRecord record = MedicalRecord.create(
                visit.getId(), "Ly do", "Trieu chung", "Tien su", "Kham", "Dien tien", "Ke hoach", "Loi dan", "Ket luan", DOCTOR_ID, NOW
        );
        record.sign("SigData", DOCTOR_ID, NOW);
        record.lock(DOCTOR_ID, NOW);

        VitalSign vs = VitalSign.create(
                visit.getId(), PATIENT_ID, null, 80, 120, 80, new BigDecimal("37.0"), 16,
                new BigDecimal("60.0"), new BigDecimal("165.0"), 98, null, DOCTOR_ID, NOW
        );

        when(vitalSignRepository.findById(VITAL_SIGN_ID)).thenReturn(Optional.of(vs));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(record));

        UpdateVitalSignCommand cmd = new UpdateVitalSignCommand(
                72, 118, 78, new BigDecimal("36.8"), 16, null, null, null, null
        );

        assertThrows(MedicalRecordAlreadyLockedException.class, () -> service.update(VITAL_SIGN_ID, cmd));
    }

    @Test
    @DisplayName("Từ chối khi không tìm thấy bản ghi chỉ số sinh tồn")
    void rejectsWhenVitalSignNotFound() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        when(vitalSignRepository.findById(VITAL_SIGN_ID)).thenReturn(Optional.empty());

        UpdateVitalSignCommand cmd = new UpdateVitalSignCommand(
                72, 118, 78, new BigDecimal("36.8"), 16, null, null, null, null
        );

        assertThrows(VitalSignNotFoundException.class, () -> service.update(VITAL_SIGN_ID, cmd));
    }

    @Test
    @DisplayName("Từ chối khi lượt khám không tồn tại")
    void rejectsWhenVisitNotFound() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        VitalSign vs = VitalSign.create(
                VISIT_ID, PATIENT_ID, null, 80, 120, 80, new BigDecimal("37.0"), 16,
                null, null, null, null, DOCTOR_ID, NOW
        );
        when(vitalSignRepository.findById(VITAL_SIGN_ID)).thenReturn(Optional.of(vs));
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.empty());

        UpdateVitalSignCommand cmd = new UpdateVitalSignCommand(
                72, 118, 78, new BigDecimal("36.8"), 16, null, null, null, null
        );

        assertThrows(VisitNotFoundException.class, () -> service.update(VITAL_SIGN_ID, cmd));
    }

    @Test
    @DisplayName("Từ chối khi bác sĩ không phụ trách lượt khám")
    void rejectsWhenDoctorNotAssigned() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        Visit visit = activeVisit();
        VitalSign vs = VitalSign.create(
                visit.getId(), PATIENT_ID, null, 80, 120, 80, new BigDecimal("37.0"), 16,
                null, null, null, null, DOCTOR_ID, NOW
        );
        when(vitalSignRepository.findById(VITAL_SIGN_ID)).thenReturn(Optional.of(vs));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
        doThrow(new MedicalRecordAccessDeniedException()).when(authorizationService).requireVisitDoctorAccess(visit.getDoctorId());

        UpdateVitalSignCommand cmd = new UpdateVitalSignCommand(
                72, 118, 78, new BigDecimal("36.8"), 16, null, null, null, null
        );

        assertThrows(MedicalRecordAccessDeniedException.class, () -> service.update(VITAL_SIGN_ID, cmd));
    }

    @Test
    @DisplayName("Từ chối khi lượt khám đã hoàn thành hoặc không còn diễn ra")
    void rejectsWhenVisitIsNotInProgress() {
        when(authorizationService.requireWriteAccess()).thenReturn(DOCTOR_ID);
        Visit visit = activeVisit();
        visit.complete(NOW.plusSeconds(3600));

        VitalSign vs = VitalSign.create(
                visit.getId(), PATIENT_ID, null, 80, 120, 80, new BigDecimal("37.0"), 16,
                null, null, null, null, DOCTOR_ID, NOW
        );
        when(vitalSignRepository.findById(VITAL_SIGN_ID)).thenReturn(Optional.of(vs));
        when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

        UpdateVitalSignCommand cmd = new UpdateVitalSignCommand(
                72, 118, 78, new BigDecimal("36.8"), 16, null, null, null, null
        );

        assertThrows(VisitInvalidStatusException.class, () -> service.update(VITAL_SIGN_ID, cmd));
    }
}

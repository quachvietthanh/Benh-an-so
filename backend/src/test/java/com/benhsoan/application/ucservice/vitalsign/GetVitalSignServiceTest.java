package com.benhsoan.application.ucservice.vitalsign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.domain.vitalsign.exception.VitalSignNotFoundException;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;
import com.benhsoan.port.outbound.repository.vitalsign.VitalSignRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class GetVitalSignServiceTest {

    @Mock
    private VitalSignRepository vitalSignRepository;
    @Mock
    private VitalSignAuthorizationService authorizationService;
    @Mock
    private MedicalRecordAccessAuditService accessAuditService;
    @Mock
    private ClockPort clockPort;

    private VitalSignResultMapper resultMapper;
    private GetVitalSignService service;

    private static final UUID VITAL_SIGN_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-20T08:00:00Z");

    @BeforeEach
    void setUp() {
        resultMapper = new VitalSignResultMapper();
        service = new GetVitalSignService(
                vitalSignRepository,
                authorizationService,
                accessAuditService,
                resultMapper,
                clockPort
        );
    }

    private VitalSign createVitalSign(UUID id, int pulse) {
        return VitalSign.create(
                VISIT_ID, PATIENT_ID, RECORD_ID, pulse, 120, 80, new BigDecimal("37.0"),
                16, new BigDecimal("60.0"), new BigDecimal("165.0"), 98, null, DOCTOR_ID, NOW
        );
    }

    @Test
    @DisplayName("Lấy chỉ số sinh tồn theo ID thành công và ghi nhận nhật ký truy cập")
    void getByIdReturnsResultAndAudits() {
        when(authorizationService.requireReadAccess()).thenReturn(DOCTOR_ID);
        when(clockPort.now()).thenReturn(NOW);

        VitalSign vs = createVitalSign(VITAL_SIGN_ID, 75);
        when(vitalSignRepository.findById(VITAL_SIGN_ID)).thenReturn(Optional.of(vs));

        VitalSignResult result = service.getById(VITAL_SIGN_ID);

        assertNotNull(result);
        assertEquals(75, result.pulse());
        assertEquals(VISIT_ID, result.visitId());
        assertEquals(PATIENT_ID, result.patientId());

        verify(accessAuditService).recordRecordView(
                eq(PATIENT_ID), eq(VISIT_ID), eq(RECORD_ID), eq(DOCTOR_ID), eq(NOW)
        );
    }

    @Test
    @DisplayName("Lấy chỉ số sinh tồn theo ID ném VitalSignNotFoundException khi không tìm thấy")
    void getByIdThrowsWhenNotFound() {
        when(authorizationService.requireReadAccess()).thenReturn(DOCTOR_ID);
        when(vitalSignRepository.findById(VITAL_SIGN_ID)).thenReturn(Optional.empty());

        assertThrows(VitalSignNotFoundException.class, () -> service.getById(VITAL_SIGN_ID));
        verify(accessAuditService, never()).recordRecordView(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Lấy chỉ số sinh tồn mới nhất theo visitId thành công và ghi log")
    void getLatestByVisitIdReturnsResult() {
        when(authorizationService.requireReadAccess()).thenReturn(DOCTOR_ID);
        when(clockPort.now()).thenReturn(NOW);

        VitalSign vs = createVitalSign(VITAL_SIGN_ID, 80);
        when(vitalSignRepository.findLatestByVisitId(VISIT_ID)).thenReturn(Optional.of(vs));

        Optional<VitalSignResult> resultOpt = service.getLatestByVisitId(VISIT_ID);

        assertTrue(resultOpt.isPresent());
        assertEquals(80, resultOpt.get().pulse());
        verify(accessAuditService).recordRecordView(
                eq(PATIENT_ID), eq(VISIT_ID), eq(RECORD_ID), eq(DOCTOR_ID), eq(NOW)
        );
    }

    @Test
    @DisplayName("Lấy chỉ số sinh tồn mới nhất theo visitId trả về rỗng khi không có bản ghi")
    void getLatestByVisitIdReturnsEmptyWhenNone() {
        when(authorizationService.requireReadAccess()).thenReturn(DOCTOR_ID);
        when(vitalSignRepository.findLatestByVisitId(VISIT_ID)).thenReturn(Optional.empty());

        Optional<VitalSignResult> resultOpt = service.getLatestByVisitId(VISIT_ID);

        assertFalse(resultOpt.isPresent());
        verify(accessAuditService, never()).recordRecordView(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Lấy danh sách chỉ số sinh tồn theo visitId thành công và ghi log cho bản ghi đầu tiên")
    void getByVisitIdReturnsListAndAuditsFirst() {
        when(authorizationService.requireReadAccess()).thenReturn(DOCTOR_ID);
        when(clockPort.now()).thenReturn(NOW);

        VitalSign vs1 = createVitalSign(UUID.randomUUID(), 75);
        VitalSign vs2 = createVitalSign(UUID.randomUUID(), 85);
        when(vitalSignRepository.findByVisitId(VISIT_ID)).thenReturn(List.of(vs1, vs2));

        List<VitalSignResult> list = service.getByVisitId(VISIT_ID);

        assertEquals(2, list.size());
        assertEquals(75, list.get(0).pulse());
        assertEquals(85, list.get(1).pulse());

        verify(accessAuditService).recordRecordView(
                eq(PATIENT_ID), eq(VISIT_ID), eq(RECORD_ID), eq(DOCTOR_ID), eq(NOW)
        );
    }

    @Test
    @DisplayName("Lấy danh sách theo visitId trả về rỗng khi không có dữ liệu")
    void getByVisitIdReturnsEmptyWhenNone() {
        when(authorizationService.requireReadAccess()).thenReturn(DOCTOR_ID);
        when(vitalSignRepository.findByVisitId(VISIT_ID)).thenReturn(List.of());

        List<VitalSignResult> list = service.getByVisitId(VISIT_ID);

        assertTrue(list.isEmpty());
        verify(accessAuditService, never()).recordRecordView(any(), any(), any(), any(), any());
    }
}

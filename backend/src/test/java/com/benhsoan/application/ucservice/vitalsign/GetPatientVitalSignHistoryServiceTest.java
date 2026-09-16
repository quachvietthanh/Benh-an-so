package com.benhsoan.application.ucservice.vitalsign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.vitalsign.VitalSignRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class GetPatientVitalSignHistoryServiceTest {

    @Mock
    private VitalSignRepository vitalSignRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private VitalSignAuthorizationService authorizationService;
    @Mock
    private MedicalRecordAccessAuditService accessAuditService;
    @Mock
    private ClockPort clockPort;

    private VitalSignResultMapper resultMapper;
    private GetPatientVitalSignHistoryService service;

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-20T08:00:00Z");

    @BeforeEach
    void setUp() {
        resultMapper = new VitalSignResultMapper();
        service = new GetPatientVitalSignHistoryService(
                vitalSignRepository,
                patientRepository,
                authorizationService,
                accessAuditService,
                resultMapper,
                clockPort
        );
    }

    @Test
    @DisplayName("TC-04: Lấy lịch sử chỉ số sinh tồn của bệnh nhân theo thứ tự thời gian và ghi log QTN-02")
    void returnsPatientVitalSignHistoryInChronologicalOrder() {
        when(authorizationService.requireReadAccess()).thenReturn(DOCTOR_ID);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(mock(Patient.class)));
        when(clockPort.now()).thenReturn(NOW);

        VitalSign vs1 = VitalSign.create(
                UUID.randomUUID(), PATIENT_ID, null, 75, 120, 80, new BigDecimal("37.0"), 16,
                new BigDecimal("60.0"), new BigDecimal("165.0"), 98, null, DOCTOR_ID, NOW.minusSeconds(86400)
        );
        VitalSign vs2 = VitalSign.create(
                UUID.randomUUID(), PATIENT_ID, null, 85, 130, 85, new BigDecimal("37.2"), 18,
                new BigDecimal("61.0"), new BigDecimal("165.0"), 98, null, DOCTOR_ID, NOW
        );

        when(vitalSignRepository.findHistoryByPatientId(PATIENT_ID)).thenReturn(List.of(vs1, vs2));

        List<VitalSignResult> history = service.getHistory(PATIENT_ID);

        assertEquals(2, history.size());
        assertEquals(75, history.get(0).pulse());
        assertEquals(85, history.get(1).pulse());

        // Verify audit log QTN-02
        verify(accessAuditService).recordHistoryView(eq(PATIENT_ID), eq(DOCTOR_ID), eq(NOW));
    }

    @Test
    @DisplayName("Ném PatientNotFoundException khi bệnh nhân không tồn tại và không gọi audit log")
    void throwsPatientNotFoundExceptionWhenPatientDoesNotExist() {
        when(authorizationService.requireReadAccess()).thenReturn(DOCTOR_ID);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.getHistory(PATIENT_ID));

        verify(accessAuditService, never()).recordHistoryView(any(), any(), any());
        verify(vitalSignRepository, never()).findHistoryByPatientId(any());
    }
}

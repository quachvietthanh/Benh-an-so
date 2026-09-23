package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordRetentionPolicy;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.PatientConsentRecord;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.ConsentHistoryStatus;
import com.benhsoan.domain.patient.exception.PatientConsentAccessDeniedException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.patient.RequestPatientDataErasureCommand;
import com.benhsoan.port.dto.result.patient.DataErasureResult;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientConsentHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestPatientDataErasureService Unit Tests (NCL-15-CN-005 / AC-03, QTN-19)")
class RequestPatientDataErasureServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");

    @Mock private PatientRepository patientRepository;
    @Mock private PatientConsentHistoryRepository patientConsentHistoryRepository;
    @Mock private MedicalRecordRetentionPolicy medicalRecordRetentionPolicy;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;
    @Mock private PatientConsentErasureAuditWriter erasureAuditWriter;
    @Mock private PatientChangeLogRepository patientChangeLogRepository;
    @Mock private PatientAccessGuard patientAccessGuard;

    private RequestPatientDataErasureService service;
    private final UUID currentUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PatientChangeDetailBuilder changeDetailBuilder = new PatientChangeDetailBuilder(new ObjectMapper());

        service = new RequestPatientDataErasureService(
                patientRepository,
                patientConsentHistoryRepository,
                medicalRecordRetentionPolicy,
                currentUserPort,
                clockPort,
                erasureAuditWriter,
                patientChangeLogRepository,
                changeDetailBuilder,
                patientAccessGuard
        );
    }

    private Patient createTestPatient(UUID patientId) {
        return Patient.restore(
                patientId,
                "PAT-2026-0005",
                "Lê Văn Cường",
                LocalDate.of(1988, 8, 18),
                Gender.MALE,
                "0912345678",
                "cuong@example.com",
                "789 Giải Phóng, Hai Bà Trưng, Hà Nội",
                "001088000003",
                "DN4010123456780",
                BloodType.B_POSITIVE,
                null, null, null, null, null, null, null, null,
                "Lê Văn Cường",
                true,
                NOW.minusSeconds(86400 * 100),
                NOW.minusSeconds(86400 * 100),
                null,
                currentUserId,
                true,
                NOW.minusSeconds(86400 * 100),
                "v1.0",
                false,
                null,
                null,
                false
        );
    }

    @Test
    @DisplayName("AC-03 & QTN-19: Xử lý yêu cầu xóa - từ chối xóa hồ sơ bệnh án theo luật và rút consent ngoài y tế")
    void requestErasure_refusesMedicalRecordDeletion_retainsRecordsAndWithdrawsConsent() {
        UUID patientId = UUID.randomUUID();
        Patient patient = createTestPatient(patientId);

        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(true);
        when(clockPort.now()).thenReturn(NOW);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(medicalRecordRetentionPolicy.retentionYears()).thenReturn(10);
        when(patientConsentHistoryRepository.getNextVersionNumber(patientId)).thenReturn(2);

        RequestPatientDataErasureCommand command = RequestPatientDataErasureCommand.builder()
                .reason("Bệnh nhân muốn xóa hết dữ liệu trên hệ thống")
                .build();

        DataErasureResult result = service.requestErasure(patientId, command);

        assertNotNull(result);
        assertEquals(patientId, result.patientId());
        assertTrue(result.medicalRecordsRetained());
        assertEquals(10, result.retentionYears());
        assertTrue(result.consentWithdrawn());
        assertTrue(result.nonMedicalUseRestricted());
        assertTrue(result.message().contains("10 năm"));
        assertTrue(result.message().contains("QTN-19"));

        // Xác nhận độc lập giao dịch: Audit log từ chối xóa được ghi qua erasureAuditWriter
        verify(erasureAuditWriter).writeErasureRefusal(
                eq(currentUserId),
                eq(patientId),
                eq("PAT-2026-0005"),
                eq(10),
                eq("Bệnh nhân muốn xóa hết dữ liệu trên hệ thống"),
                eq(NOW)
        );

        // Xác nhận bản ghi lịch sử WITHDRAWN được lưu
        ArgumentCaptor<PatientConsentRecord> historyCaptor = ArgumentCaptor.forClass(PatientConsentRecord.class);
        verify(patientConsentHistoryRepository).save(historyCaptor.capture());
        PatientConsentRecord savedHistory = historyCaptor.getValue();
        assertEquals(ConsentHistoryStatus.WITHDRAWN, savedHistory.getStatus());
        assertTrue(savedHistory.isConsentWithdrawn());
        assertTrue(savedHistory.isNonMedicalUseRestricted());
        assertEquals("Bệnh nhân muốn xóa hết dữ liệu trên hệ thống", savedHistory.getConsentWithdrawnReason());

        // Xác nhận nhật ký thay đổi thông tin bệnh nhân
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
    }

    @Test
    @DisplayName("Security: Từ chối yêu cầu xóa dữ liệu nếu người dùng không được phân quyền")
    void requestErasure_unauthorized_throwsAccessDeniedException() {
        UUID patientId = UUID.randomUUID();

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(false);
        doThrow(new AccessDeniedException("Access denied")).when(patientAccessGuard).requirePatientOwnership(patientId);

        RequestPatientDataErasureCommand command = RequestPatientDataErasureCommand.builder()
                .reason("Yêu cầu xóa dữ liệu")
                .build();

        assertThrows(PatientConsentAccessDeniedException.class, () -> service.requestErasure(patientId, command));
    }

    @Test
    @DisplayName("Edge Case: Bệnh nhân không tồn tại ném PatientNotFoundException")
    void requestErasure_patientNotFound_throwsException() {
        UUID patientId = UUID.randomUUID();

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        RequestPatientDataErasureCommand command = RequestPatientDataErasureCommand.builder()
                .reason("Yêu cầu xóa dữ liệu")
                .build();

        assertThrows(PatientNotFoundException.class, () -> service.requestErasure(patientId, command));
    }
}

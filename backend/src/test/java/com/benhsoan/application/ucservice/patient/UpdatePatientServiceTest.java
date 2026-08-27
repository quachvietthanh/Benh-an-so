package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.exception.PatientConsentAccessDeniedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.patient.UpdatePatientCommand;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePatientService - Unit Tests (NCL-15-CN-001 / TC-03, TC-04, P1, P2)")
class UpdatePatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PatientChangeLogRepository patientChangeLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AuditLogRepository auditLogRepository;

    private UpdatePatientService service;
    private final UUID currentUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PatientChangeDetailBuilder changeDetailBuilder = new PatientChangeDetailBuilder(new ObjectMapper());
        PatientResultMapper patientResultMapper = new PatientResultMapper();

        service = new UpdatePatientService(
                patientRepository,
                patientChangeLogRepository,
                currentUserPort,
                patientResultMapper,
                changeDetailBuilder,
                auditLogRepository
        );

        lenient().when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
    }

    @Test
    @DisplayName("TC-03: Người có quyền PATIENT_CONSENT_UPDATE rút lại sự đồng ý thành công")
    void withdrawsConsentSuccessfullyWhenAuthorized() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1995, 5, 10),
                Gender.MALE,
                "0909000001",
                "a@example.com",
                "123 Street",
                "079095001234",
                "DN4790123456789",
                BloodType.O_POSITIVE,
                "Nguyen Van B",
                "0909998877",
                true,
                "v1.0",
                currentUserId
        );

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(true);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .active(true)
                .consentWithdrawn(true)
                .consentWithdrawnReason("Khong muon nhan khao sat hay thong bao ngoai KCB")
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertTrue(result.consentWithdrawn());
        assertNotNull(result.consentWithdrawnAt());
        assertEquals("Khong muon nhan khao sat hay thong bao ngoai KCB", result.consentWithdrawnReason());
        assertTrue(result.nonMedicalUseRestricted());
        assertTrue(result.active(), "Hồ sơ vẫn active cho khám chữa bệnh");

        verify(patientRepository).save(any(Patient.class));
        verify(patientRepository).findByIdForUpdate(patientId);
        verify(patientRepository, never()).findById(patientId);
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("P1/P2: Người dùng có PATIENT_UPDATE nhưng KHÔNG CÓ PATIENT_CONSENT_UPDATE bị từ chối khi sửa consent")
    void rejectsConsentModificationWhenUserLacksConsentPermission() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1995, 5, 10),
                Gender.MALE,
                "0909000001",
                "a@example.com",
                "123 Street",
                "079095001234",
                "DN4790123456789",
                BloodType.O_POSITIVE,
                "Nguyen Van B",
                "0909998877",
                true,
                "v1.0",
                currentUserId
        );

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(false);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .active(true)
                .consentWithdrawn(true)
                .consentWithdrawnReason("Tu y rut consent ma khong co quyen")
                .build();

        assertThrows(PatientConsentAccessDeniedException.class, () -> service.update(patientId, command));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Bác sĩ/Người dùng cập nhật thông tin y tế/liên hệ mà không sửa consent thì không cần PATIENT_CONSENT_UPDATE")
    void updatesProfileWithoutModifyingConsentDoesNotRequireConsentPermission() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1995, 5, 10),
                Gender.MALE,
                "0909000001",
                "a@example.com",
                "123 Street",
                "079095001234",
                "DN4790123456789",
                BloodType.O_POSITIVE,
                "Nguyen Van B",
                "0909998877",
                true,
                "v1.0",
                currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A Updated")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000002")
                .address("456 New Street")
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertEquals("Nguyen Van A Updated", result.fullName());
        assertEquals("0909000002", result.phone());
        verify(patientRepository).save(any(Patient.class));
        verify(currentUserPort, never()).hasPermission("PATIENT_CONSENT_UPDATE");
    }

    @Test
    @DisplayName("Endpoint chuyên biệt: Cập nhật consent khi fullName=null thành công")
    void updatesConsentOnlyWhenFullNameIsNull() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001",
                "Nguyen Van A",
                LocalDate.of(1995, 5, 10),
                Gender.MALE,
                "0909000001",
                "a@example.com",
                "123 Street",
                "079095001234",
                "DN4790123456789",
                BloodType.O_POSITIVE,
                "Nguyen Van B",
                "0909998877",
                true,
                "v1.0",
                currentUserId
        );

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(true);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .consentWithdrawn(true)
                .consentWithdrawnReason("Rut consent qua dedicated endpoint")
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertTrue(result.consentWithdrawn());
        assertEquals("Rut consent qua dedicated endpoint", result.consentWithdrawnReason());
        assertTrue(result.nonMedicalUseRestricted());
        assertEquals("Nguyen Van A", result.fullName());
    }

    @Test
    @DisplayName("QTN-24: consentWithdrawn=false không tự gia hạn nếu chưa ghi nhận consentAgreed=true")
    void rejectsRenewalWithoutNewConsent() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Nguyen Van B", "0909998877",
                true, "v1.0", currentUserId
        );
        existing.withdrawConsent("Nguoi benh da rut consent", null);

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(true);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .active(true)
                .consentWithdrawn(false)
                .build();

        assertThrows(ValidationException.class, () -> service.update(patientId, command));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("QTN-24: Gia hạn consent cần xác nhận mới và phiên bản v1.0")
    void renewsConsentWithNewAgreementAndSupportedVersion() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Nguyen Van B", "0909998877",
                true, "v1.0", currentUserId
        );
        existing.withdrawConsent("Nguoi benh da rut consent", null);

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(true);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .active(true)
                .consentWithdrawn(false)
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        PatientResult result = service.update(patientId, command);

        assertTrue(result.consentAgreed());
        assertFalse(result.consentWithdrawn());
        assertEquals("v1.0", result.consentVersion());
        ArgumentCaptor<PatientChangeLog> changeLogCaptor = ArgumentCaptor.forClass(PatientChangeLog.class);
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(patientChangeLogRepository).save(changeLogCaptor.capture());
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertTrue(changeLogCaptor.getValue().getChangeDetail().contains("\"consentWithdrawnAt\""));
        assertTrue(auditLogCaptor.getValue().getDetail().contains("\"consentVersion\":\"v1.0\""));
    }

    @Test
    @DisplayName("QTN-24: Gia hạn consent từ chối phiên bản phiếu không được hỗ trợ")
    void rejectsRenewalWithUnsupportedConsentVersion() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Nguyen Van B", "0909998877",
                true, "v1.0", currentUserId
        );
        existing.withdrawConsent("Nguoi benh da rut consent", null);

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(true);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .active(true)
                .consentWithdrawn(false)
                .consentAgreed(true)
                .consentVersion("v2.0")
                .build();

        assertThrows(ValidationException.class, () -> service.update(patientId, command));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Finding 1 Fix: Bác sĩ gửi payload chứa consent nguyên trạng không bị chặn bởi PATIENT_CONSENT_UPDATE")
    void updatesProfileWithUnchangedConsentFieldsDoesNotRequireConsentPermission() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Nguyen Van B", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A Updated")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000002")
                .address("456 New Street")
                .active(true)
                .consentAgreed(true)
                .consentWithdrawn(false)
                .consentVersion("v1.0")
                .consentWithdrawnReason(null)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertEquals("Nguyen Van A Updated", result.fullName());
        assertEquals("0909000002", result.phone());
        verify(patientRepository).save(any(Patient.class));
        verify(currentUserPort, never()).hasPermission("PATIENT_CONSENT_UPDATE");
    }

    @Test
    @DisplayName("Finding 2 Fix: Cập nhật consentWithdrawnReason khi đã rút consent thành công và giữ nguyên thời điểm rút")
    void updatesWithdrawalReasonWhenAlreadyWithdrawn() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Nguyen Van B", "0909998877",
                true, "v1.0", currentUserId
        );
        java.time.Instant initialWithdrawal = java.time.Instant.parse("2026-08-20T10:00:00Z");
        existing.withdrawConsent("Lý do ban đầu", initialWithdrawal);

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(true);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .consentWithdrawn(true)
                .consentWithdrawnReason("Lý do mới bổ sung chi tiết")
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertTrue(result.consentWithdrawn());
        assertEquals("Lý do mới bổ sung chi tiết", result.consentWithdrawnReason());
        assertEquals(initialWithdrawal, result.consentWithdrawnAt());
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Finding 2 Fix: Người dùng thiếu PATIENT_CONSENT_UPDATE cố ý đổi lý do rút consent bị từ chối 403")
    void rejectsConsentReasonModificationWhenUserLacksConsentPermission() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Nguyen Van B", "0909998877",
                true, "v1.0", currentUserId
        );
        existing.withdrawConsent("Lý do ban đầu", java.time.Instant.now());

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(false);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .active(true)
                .consentWithdrawn(true)
                .consentWithdrawnReason("Lý do mới không có quyền sửa")
                .build();

        assertThrows(PatientConsentAccessDeniedException.class, () -> service.update(patientId, command));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Finding 3 Fix: identityNumber là chuỗi rỗng/khoảng trắng được chuẩn hóa thành null và không bị báo trùng")
    void normalizesBlankIdentityNumberToNullOnUpdate() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Nguyen Van B", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .identityNumber("   ")
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        verify(patientRepository, never()).existsByIdentityNumberAndIdNot(any(), any());
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("TC-03: Từ chối consentAgreed=false khi không yêu cầu rút consent")
    void rejectsFalseConsentWithoutWithdrawalRequest() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Nguyen Van B", "0909998877",
                true, "v1.0", currentUserId
        );

        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(true);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .consentAgreed(false)
                .build();

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.update(patientId, command)
        );

        assertEquals(
                "consentAgreed=false requires consentWithdrawn=true to withdraw consent.",
                exception.getMessage()
        );
        verify(patientRepository, never()).save(any(Patient.class));
    }
}

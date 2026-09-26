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
import com.benhsoan.domain.patient.exception.PatientAlreadyMergedException;
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
    @Mock private com.benhsoan.port.outbound.repository.patient.PatientConsentHistoryRepository patientConsentHistoryRepository;
    @Mock private com.benhsoan.port.outbound.time.ClockPort clockPort;
    @Mock private com.benhsoan.port.outbound.repository.auth.UserRepository userRepository;
    @Mock private com.benhsoan.port.outbound.repository.auth.RoleRepository roleRepository;

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
                auditLogRepository,
                patientConsentHistoryRepository,
                clockPort,
                userRepository,
                roleRepository
        );

        lenient().when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
        lenient().when(clockPort.now()).thenReturn(java.time.Instant.parse("2026-09-24T10:00:00Z"));
        lenient().when(patientConsentHistoryRepository.getNextVersionNumber(any())).thenReturn(2);
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

    @Test
    @DisplayName("NCL-02-CN-007 TC-01 & TC-03: Cập nhật thông tin người liên hệ khẩn cấp đầy đủ và ghi nhật ký thay đổi")
    void updatesEmergencyContactAndRelationshipSuccessfully() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Le Thi B", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("Tran Van C")
                .emergencyRelationship("Bố")
                .emergencyPhone("+84908887766")
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertEquals("Tran Van C", result.emergencyContact());
        assertEquals("Bố", result.emergencyRelationship());
        assertEquals("0908887766", result.emergencyPhone());

        verify(patientRepository).save(any(Patient.class));
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
    }

    @Test
    @DisplayName("NCL-02-CN-007 TC-02: Từ chối cập nhật khi số điện thoại người liên hệ khẩn cấp sai định dạng")
    void rejectsUpdateWhenEmergencyPhoneIsInvalid() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Le Thi B", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("Tran Van C")
                .emergencyRelationship("Bố")
                .emergencyPhone("invalid-phone")
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertTrue(ex.getMessage().contains("emergencyPhone"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-007 QTN-43: Bảo vệ dữ liệu gốc khi client gửi lại giá trị đã che trong chế độ ẩn danh")
    void preservesExistingEmergencyValuesWhenMaskedFromDemoMode() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Le Thi B", "Mẹ", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Client read data with anonymization mode on, so emergencyContact was "BỆNH NHÂN" and emergencyPhone was "09******77"
        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("BỆNH NHÂN")
                .emergencyRelationship("Mẹ")
                .emergencyPhone("09******77")
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertEquals("Le Thi B", result.emergencyContact());
        assertEquals("0909998877", result.emergencyPhone());
        assertEquals("Mẹ", result.emergencyRelationship());
    }

    @Test
    @DisplayName("P2.03 Cohesive Triplet: Cho phép xóa trắng người liên hệ khẩn cấp khi gửi cả 3 trường null và lưu vết lịch sử")
    void allowsClearingEmergencyContactWhenAllThreeFieldsAreNullAndLogsChange() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Le Thi B", "Mẹ", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact(null)
                .emergencyRelationship(null)
                .emergencyPhone(null)
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertNull(result.emergencyContact());
        org.junit.jupiter.api.Assertions.assertNull(result.emergencyRelationship());
        org.junit.jupiter.api.Assertions.assertNull(result.emergencyPhone());

        verify(patientRepository).save(any(Patient.class));
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
    }

    @Test
    @DisplayName("P2.03 Cohesive Triplet: Từ chối xóa một phần khi chỉ xóa SĐT nhưng vẫn để lại họ tên người liên hệ")
    void rejectsPartialClearingWhenOnlyEmergencyContactProvided() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Le Thi B", "Mẹ", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("Le Thi B")
                .emergencyRelationship(null)
                .emergencyPhone(null)
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertTrue(ex.getMessage().contains("emergencyRelationship"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("F-1 Cohesive Triplet: Từ chối cập nhật khi có tên và SĐT nhưng bỏ trống quan hệ (null)")
    void rejectsUpdateWhenContactAndPhoneProvidedWithoutRelationship() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Le Thi B", "Mẹ", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("Tran Van C")
                .emergencyRelationship(null)
                .emergencyPhone("0912345678")
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertTrue(ex.getMessage().contains("emergencyRelationship"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("F-1 Cohesive Triplet: Từ chối cập nhật khi quan hệ là khoảng trắng")
    void rejectsUpdateWhenRelationshipIsBlankWhitespace() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, null, null, null,
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("Tran Van C")
                .emergencyRelationship("   ")
                .emergencyPhone("0912345678")
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertTrue(ex.getMessage().contains("emergencyRelationship"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("F-1 Cohesive Triplet: Từ chối cập nhật khi có tên và quan hệ nhưng bỏ trống SĐT")
    void rejectsUpdateWhenContactAndRelationshipProvidedWithoutPhone() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Le Thi B", "Mẹ", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("Tran Van C")
                .emergencyRelationship("Bố")
                .emergencyPhone(null)
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertTrue(ex.getMessage().contains("emergencyPhone"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("F-1 Cohesive Triplet: Từ chối cập nhật khi có quan hệ và SĐT nhưng bỏ trống tên")
    void rejectsUpdateWhenRelationshipAndPhoneProvidedWithoutContact() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Le Thi B", "Mẹ", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact(null)
                .emergencyRelationship("Bố")
                .emergencyPhone("0912345678")
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertTrue(ex.getMessage().contains("emergencyContact"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("F-1 Cohesive Triplet: Từ chối cập nhật khi chỉ cung cấp SĐT")
    void rejectsUpdateWhenOnlyEmergencyPhoneProvided() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, null, null, null,
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact(null)
                .emergencyRelationship(null)
                .emergencyPhone("0912345678")
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertTrue(ex.getMessage().contains("emergencyContact"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("F-1 Cohesive Triplet: Từ chối cập nhật khi chỉ cung cấp quan hệ")
    void rejectsUpdateWhenOnlyEmergencyRelationshipProvided() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, null, null, null,
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact(null)
                .emergencyRelationship("Bố")
                .emergencyPhone(null)
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertTrue(ex.getMessage().contains("emergencyContact"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("F-1 Cohesive Triplet: Cập nhật thành công khi cung cấp đầy đủ cả 3 trường hợp lệ")
    void updatesSuccessfullyWhenAllThreeEmergencyFieldsProvided() {
        UUID patientId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000001", "Nguyen Van A", LocalDate.of(1995, 5, 10), Gender.MALE,
                "0909000001", "a@example.com", "123 Street", "079095001234",
                "DN4790123456789", BloodType.O_POSITIVE, "Le Thi B", "Mẹ", "0909998877",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("Tran Van C")
                .emergencyRelationship("Bố")
                .emergencyPhone("0912345678")
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertEquals("Tran Van C", result.emergencyContact());
        assertEquals("Bố", result.emergencyRelationship());
        assertEquals("0912345678", result.emergencyPhone());

        verify(patientRepository).save(any(Patient.class));
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
    }

    @Test
    @DisplayName("NCL-02-CN-008: Cập nhật thông tin người giám hộ thành công cho bệnh nhân trẻ em")
    void updatesPediatricPatientGuardianSuccessfully() {
        UUID patientId = UUID.randomUUID();
        LocalDate childDob = LocalDate.now().minusYears(6);
        Patient existing = Patient.create(
                "BN000005", "Nguyen Van Con", childDob, Gender.MALE,
                null, null, "123 Street", null,
                null, BloodType.UNKNOWN, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null, "Nguyen Van Bo",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van Con")
                .dateOfBirth(childDob)
                .gender(Gender.MALE)
                .guardianName("Nguyen Thi Me")
                .guardianRelationship("Mẹ")
                .guardianPhone("0987654321")
                .guardianIdentityNumber("001200000002")
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertTrue(result.isMinor());
        assertEquals("Nguyen Thi Me", result.guardianName());
        assertEquals("Mẹ", result.guardianRelationship());
        assertEquals("0987654321", result.guardianPhone());
        assertEquals("001200000002", result.guardianIdentityNumber());
        assertEquals("Nguyen Thi Me", result.consentSignerName());

        verify(patientRepository).save(any(Patient.class));
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-02: Chặn xóa người giám hộ khi bệnh nhân vẫn dưới 18 tuổi")
    void rejectsClearingGuardianWhenPatientIsStillMinor() {
        UUID patientId = UUID.randomUUID();
        LocalDate childDob = LocalDate.now().minusYears(8);
        Patient existing = Patient.create(
                "BN000005", "Nguyen Van Con", childDob, Gender.MALE,
                null, null, "123 Street", null,
                null, BloodType.UNKNOWN, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null, "Nguyen Van Bo",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van Con")
                .dateOfBirth(childDob)
                .gender(Gender.MALE)
                .guardianName(null)
                .guardianRelationship(null)
                .guardianPhone(null)
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertTrue(ex.getMessage().contains("guardianName"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-04: Bệnh nhân đủ 18 tuổi chuyển tiếp thành niên thành công, gỡ người giám hộ và đứng tên phiếu đồng ý")
    void transitionsToAdultSuccessfullyWhenPatientReaches18() {
        UUID patientId = UUID.randomUUID();
        LocalDate adultDob = LocalDate.now().minusYears(18);
        Patient existing = Patient.create(
                "BN000006", "Nguyen Van Truong Thanh", adultDob, Gender.MALE,
                "0901234567", "tt@example.com", "123 Street", "079095009999",
                null, BloodType.O_POSITIVE, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null, "Nguyen Van Bo",
                true, "v1.0", currentUserId
        );

        // User only has PATIENT_UPDATE, not PATIENT_CONSENT_UPDATE
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van Truong Thanh")
                .dateOfBirth(adultDob)
                .gender(Gender.MALE)
                .phone("0901234567")
                .email("tt@example.com")
                .address("123 Street")
                .identityNumber("079095009999")
                .transitionToAdult(true)
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertFalse(result.isMinor());
        assertFalse(result.requiresAdultTransitionPrompt());
        org.junit.jupiter.api.Assertions.assertNull(result.guardianName());
        org.junit.jupiter.api.Assertions.assertNull(result.guardianRelationship());
        org.junit.jupiter.api.Assertions.assertNull(result.guardianPhone());
        org.junit.jupiter.api.Assertions.assertNull(result.guardianIdentityNumber());
        assertEquals("Nguyen Van Truong Thanh", result.consentSignerName(), "TC-04: Sau khi chuyển tiếp thành niên, bệnh nhân tự đứng tên phiếu đồng ý");
        assertTrue(result.consentAgreed());
        assertFalse(result.consentWithdrawn());

        verify(patientRepository).save(any(Patient.class));
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("P1 / TC-04: Bệnh nhân đủ 18 tuổi chuyển tiếp thành niên bảo toàn trạng thái rút consent (consentWithdrawn=true)")
    void transitionsToAdultPreservesConsentWithdrawalWhenPatientHadWithdrawnConsent() {
        UUID patientId = UUID.randomUUID();
        LocalDate adultDob = LocalDate.now().minusYears(18);
        Patient existing = Patient.create(
                "BN000006", "Nguyen Van Truong Thanh", adultDob, Gender.MALE,
                "0901234567", "tt@example.com", "123 Street", "079095009999",
                null, BloodType.O_POSITIVE, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null, "Nguyen Van Bo",
                true, "v1.0", currentUserId
        );
        existing.withdrawConsent("Không muốn dùng dữ liệu ngoài y tế", java.time.Instant.now());

        // User only has PATIENT_UPDATE, not PATIENT_CONSENT_UPDATE
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van Truong Thanh")
                .dateOfBirth(adultDob)
                .gender(Gender.MALE)
                .phone("0901234567")
                .email("tt@example.com")
                .address("123 Street")
                .identityNumber("079095009999")
                .transitionToAdult(true)
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertFalse(result.isMinor());
        assertFalse(result.requiresAdultTransitionPrompt());
        org.junit.jupiter.api.Assertions.assertNull(result.guardianName());
        assertEquals("Nguyen Van Truong Thanh", result.consentSignerName());
        assertTrue(result.consentWithdrawn(), "Trạng thái rút consent không được tự động xóa khi transitionToAdult");
        assertTrue(result.nonMedicalUseRestricted(), "Hạn chế sử dụng dữ liệu phi y tế phải được giữ nguyên");
        assertEquals("Không muốn dùng dữ liệu ngoài y tế", result.consentWithdrawnReason());

        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("P1: Chặn khôi phục consent khi transitionToAdult nếu user không có quyền PATIENT_CONSENT_UPDATE")
    void rejectsConsentRenewalDuringTransitionToAdultWhenUserLacksConsentPermission() {
        UUID patientId = UUID.randomUUID();
        LocalDate adultDob = LocalDate.now().minusYears(18);
        Patient existing = Patient.create(
                "BN000006", "Nguyen Van Truong Thanh", adultDob, Gender.MALE,
                "0901234567", "tt@example.com", "123 Street", "079095009999",
                null, BloodType.O_POSITIVE, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null, "Nguyen Van Bo",
                true, "v1.0", currentUserId
        );
        existing.withdrawConsent("Không muốn dùng dữ liệu ngoài y tế", java.time.Instant.now());

        // User does NOT have PATIENT_CONSENT_UPDATE
        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(false);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van Truong Thanh")
                .dateOfBirth(adultDob)
                .gender(Gender.MALE)
                .phone("0901234567")
                .transitionToAdult(true)
                .consentAgreed(true)
                .consentWithdrawn(false)
                .consentVersion("v1.0")
                .active(true)
                .build();

        assertThrows(
                PatientConsentAccessDeniedException.class,
                () -> service.update(patientId, command)
        );

        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("P1: Cho phép chuyển tiếp thành niên và gia hạn consent thành công khi user có đủ quyền PATIENT_CONSENT_UPDATE")
    void allowsConsentRenewalDuringTransitionToAdultWhenUserHasConsentPermission() {
        UUID patientId = UUID.randomUUID();
        LocalDate adultDob = LocalDate.now().minusYears(18);
        Patient existing = Patient.create(
                "BN000006", "Nguyen Van Truong Thanh", adultDob, Gender.MALE,
                "0901234567", "tt@example.com", "123 Street", "079095009999",
                null, BloodType.O_POSITIVE, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null, "Nguyen Van Bo",
                true, "v1.0", currentUserId
        );
        existing.withdrawConsent("Không muốn dùng dữ liệu ngoài y tế", java.time.Instant.now());

        // User HAS PATIENT_CONSENT_UPDATE
        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(true);
        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van Truong Thanh")
                .dateOfBirth(adultDob)
                .gender(Gender.MALE)
                .phone("0901234567")
                .transitionToAdult(true)
                .consentAgreed(true)
                .consentWithdrawn(false)
                .consentVersion("v1.0")
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertFalse(result.isMinor());
        org.junit.jupiter.api.Assertions.assertNull(result.guardianName());
        assertEquals("Nguyen Van Truong Thanh", result.consentSignerName());
        assertTrue(result.consentAgreed());
        assertFalse(result.consentWithdrawn());
        assertFalse(result.nonMedicalUseRestricted());
        org.junit.jupiter.api.Assertions.assertNull(result.consentWithdrawnReason());

        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-04: Từ chối chuyển tiếp thành niên khi bệnh nhân chưa đủ 18 tuổi")
    void rejectsTransitionToAdultWhenPatientIsStillMinor() {
        UUID patientId = UUID.randomUUID();
        LocalDate minorDob = LocalDate.now().minusYears(16);
        Patient existing = Patient.create(
                "BN000007", "Nguyen Van Chua Lon", minorDob, Gender.MALE,
                null, null, "123 Street", null,
                null, BloodType.UNKNOWN, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null, "Nguyen Van Bo",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van Chua Lon")
                .dateOfBirth(minorDob)
                .gender(Gender.MALE)
                .transitionToAdult(true)
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertTrue(ex.getMessage().contains("transitionToAdult") || ex.getMessage().contains("18 tuổi"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("P2-1 / TC-04: Từ chối gỡ bỏ người giám hộ cho bệnh nhân đủ 18 tuổi nếu không kích hoạt transitionToAdult")
    void rejectsClearingGuardianForAdultPatientWithoutTransitionToAdult() {
        UUID patientId = UUID.randomUUID();
        LocalDate adultDob = LocalDate.now().minusYears(19);
        Patient existing = Patient.create(
                "BN000010", "Nguyen Van Lon Roi", adultDob, Gender.MALE,
                "0901234567", "adult@example.com", "123 Street", "079095001234",
                null, BloodType.O_POSITIVE, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", "001200000001", null, "Nguyen Van Bo",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        // Client attempts to clear guardian by sending guardianName = null, without transitionToAdult
        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van Lon Roi")
                .dateOfBirth(adultDob)
                .gender(Gender.MALE)
                .phone("0901234567")
                .guardianName(null)
                .guardianRelationship(null)
                .guardianPhone(null)
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertEquals("transitionToAdult", ex.getField());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("P1-2 / QTN-44: Từ chối cập nhật bệnh nhân trẻ em khi consentSignerName khác người giám hộ")
    void rejectsUpdatingMinorPatientWhenConsentSignerMismatchGuardian() {
        UUID patientId = UUID.randomUUID();
        LocalDate childDob = LocalDate.now().minusYears(8);
        Patient existing = Patient.create(
                "BN000011", "Nguyen Van Be", childDob, Gender.MALE,
                null, null, "123 Street", null,
                null, BloodType.UNKNOWN, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", null, null, "Nguyen Van Bo",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van Be")
                .dateOfBirth(childDob)
                .gender(Gender.MALE)
                .guardianName("Nguyen Van Bo")
                .guardianRelationship("Bố")
                .guardianPhone("0912345678")
                .consentSignerName("Người Lạ Ký")
                .active(true)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.update(patientId, command));
        assertEquals("consentSignerName", ex.getField());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("P3-2: Giữ nguyên người giám hộ khi client round-trip nhãn ẩn danh dạng GIÁM HỘ #BN...")
    void preservesGuardianNameWhenRoundTrippingMaskedGuardianName() {
        UUID patientId = UUID.randomUUID();
        LocalDate childDob = LocalDate.now().minusYears(8);
        Patient existing = Patient.create(
                "BN000012", "Nguyen Van Be", childDob, Gender.MALE,
                null, null, "123 Street", null,
                null, BloodType.UNKNOWN, null, null, null,
                "Nguyen Van Bo", "Bố", "0912345678", null, null, "Nguyen Van Bo",
                true, "v1.0", currentUserId
        );

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Client read anonymized data and sent back "GIÁM HỘ #BN000012"
        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van Be")
                .dateOfBirth(childDob)
                .gender(Gender.MALE)
                .guardianName("GIÁM HỘ #BN000012")
                .guardianRelationship("Bố")
                .guardianPhone("0912345678")
                .active(true)
                .build();

        PatientResult result = service.update(patientId, command);

        assertNotNull(result);
        assertEquals("Nguyen Van Bo", result.guardianName(), "Giá trị guardianName thật không bị ghi đè bởi nhãn ẩn danh");
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-006-TC-03: Chặn cập nhật hồ sơ đã gộp (Read-only)")
    void rejectsUpdateOnMergedPatient() {
        UUID patientId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Patient existing = Patient.create(
                "BN000099", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                "0901234567", null, "123 Street", null,
                null, BloodType.UNKNOWN, null, null, null,
                null, null, null, null, null, null,
                true, "v1.0", currentUserId
        );
        existing.markAsMerged(targetId, currentUserId, "Gộp hồ sơ trùng");

        when(patientRepository.findByIdForUpdate(patientId)).thenReturn(Optional.of(existing));

        UpdatePatientCommand command = UpdatePatientCommand.builder()
                .fullName("Nguyen Van A Updated")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .active(true)
                .build();

        assertThrows(PatientAlreadyMergedException.class, () -> service.update(patientId, command));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    // ------------------------------------------------------------------
    // NCL-14-CN-010: staff-only guardian assignment via PUT /patients/{patientId}
    // ------------------------------------------------------------------

    private static final LocalDate MINOR_DOB = LocalDate.of(2015, 5, 10);
    private static final LocalDate ADULT_DOB = LocalDate.of(1995, 5, 10);

    private Patient restoredPatient(
            UUID patientId,
            LocalDate dateOfBirth,
            UUID portalUserId,
            String guardianName,
            UUID storedGuardianUserId
    ) {
        return Patient.restore(
                patientId, "BN000002", "Nguyen Van Con", dateOfBirth, Gender.MALE,
                null, null, "123 Street", null, null, BloodType.O_POSITIVE,
                "Nguyen Van Cha", "Bo", "0909998877",
                guardianName, "Bo", "0909998877", null,
                storedGuardianUserId, guardianName,
                true, java.time.Instant.parse("2026-01-01T00:00:00Z"),
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                portalUserId, currentUserId,
                true, java.time.Instant.parse("2026-01-01T00:00:00Z"), "v1.0",
                false, null, null, false);
    }

    private UpdatePatientCommand.UpdatePatientCommandBuilder guardianCommand(LocalDate dateOfBirth) {
        return UpdatePatientCommand.builder()
                .fullName("Nguyen Van Con")
                .dateOfBirth(dateOfBirth)
                .gender(Gender.MALE)
                .guardianName("Nguyen Van Cha")
                .guardianRelationship("Bo")
                .guardianPhone("0909998877")
                .active(true);
    }

    private com.benhsoan.domain.auth.User guardianUser(UUID userId, UUID roleId, boolean active) {
        return com.benhsoan.domain.auth.User.restore(
                userId, "guardian1", "hash", "Nguyen Van Cha", "guardian1@example.com",
                "0909998877", roleId, active, null,
                java.time.Instant.parse("2026-01-01T00:00:00Z"));
    }

    private com.benhsoan.domain.auth.Role patientRole(UUID roleId) {
        return com.benhsoan.domain.auth.Role.restore(
                roleId, "PATIENT", null, true,
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                java.time.Instant.parse("2026-01-01T00:00:00Z"), java.util.Set.of());
    }

    @Test
    @DisplayName("NCL-14-CN-010: gan nguoi giam ho hop le cho ho so chua thanh nien")
    void assignsValidGuardianUserId() {
        UUID patientId = UUID.randomUUID();
        UUID guardianUserId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientId))
                .thenReturn(Optional.of(restoredPatient(patientId, MINOR_DOB, null, "Nguyen Van Cha", null)));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(guardianUserId))
                .thenReturn(Optional.of(guardianUser(guardianUserId, roleId, true)));
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(patientRole(roleId)));
        when(patientRepository.findByUserId(guardianUserId)).thenReturn(Optional.empty());

        PatientResult result = service.update(
                patientId,
                guardianCommand(MINOR_DOB).guardianUserId(guardianUserId).build());

        assertEquals(guardianUserId, result.guardianUserId());
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-14-CN-010: tai khoan khong co vai tro PATIENT khong the lam nguoi giam ho")
    void rejectsGuardianWithoutPatientRole() {
        UUID patientId = UUID.randomUUID();
        UUID guardianUserId = UUID.randomUUID();
        UUID patientRoleId = UUID.randomUUID();
        UUID adminRoleId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientId))
                .thenReturn(Optional.of(restoredPatient(patientId, MINOR_DOB, null, "Nguyen Van Cha", null)));
        when(userRepository.findById(guardianUserId))
                .thenReturn(Optional.of(guardianUser(guardianUserId, adminRoleId, true)));
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(patientRole(patientRoleId)));

        assertThrows(ValidationException.class, () -> service.update(
                patientId,
                guardianCommand(MINOR_DOB).guardianUserId(guardianUserId).build()));

        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-14-CN-010: tai khoan nguoi giam ho bi vo hieu hoa bi tu choi")
    void rejectsInactiveGuardian() {
        UUID patientId = UUID.randomUUID();
        UUID guardianUserId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientId))
                .thenReturn(Optional.of(restoredPatient(patientId, MINOR_DOB, null, "Nguyen Van Cha", null)));
        when(userRepository.findById(guardianUserId))
                .thenReturn(Optional.of(guardianUser(guardianUserId, roleId, false)));

        assertThrows(ValidationException.class, () -> service.update(
                patientId,
                guardianCommand(MINOR_DOB).guardianUserId(guardianUserId).build()));

        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-14-CN-010: khong tim thay tai khoan nguoi giam ho thi tu choi")
    void rejectsUnknownGuardianUser() {
        UUID patientId = UUID.randomUUID();
        UUID guardianUserId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientId))
                .thenReturn(Optional.of(restoredPatient(patientId, MINOR_DOB, null, "Nguyen Van Cha", null)));
        when(userRepository.findById(guardianUserId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.update(
                patientId,
                guardianCommand(MINOR_DOB).guardianUserId(guardianUserId).build()));

        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-14-CN-010: benh nhan khong the tu gan chinh minh lam nguoi giam ho")
    void rejectsSelfGuardianLink() {
        UUID patientId = UUID.randomUUID();
        UUID ownPortalUserId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientId))
                .thenReturn(Optional.of(restoredPatient(patientId, MINOR_DOB, ownPortalUserId, "Nguyen Van Cha", null)));

        assertThrows(ValidationException.class, () -> service.update(
                patientId,
                guardianCommand(MINOR_DOB).guardianUserId(ownPortalUserId).build()));

        verify(userRepository, never()).findById(any());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-14-CN-010: chan lien ket giam ho vong (A <-> B) voi ID dung kieu (P1.1)")
    void rejectsGuardianCycle() {
        // Realistic data model: a patient id, a patient-portal user id, and a guardian account id
        // are three DIFFERENT identifiers. guardian_user_id references users(id), so the cycle
        // guard must compare users.id values only.
        UUID patientAId = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID patientBId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientAId))
                .thenReturn(Optional.of(restoredPatient(patientAId, MINOR_DOB, userA, "Nguyen Van Cha", null)));
        when(userRepository.findById(userB))
                .thenReturn(Optional.of(guardianUser(userB, roleId, true)));
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(patientRole(roleId)));
        // B's guardian account is A's portal account => A -> B -> A would be created.
        when(patientRepository.findByUserId(userB))
                .thenReturn(Optional.of(restoredPatient(patientBId, MINOR_DOB, userB, "Nguyen Van Cha", userA)));

        assertThrows(ValidationException.class, () -> service.update(
                patientAId,
                guardianCommand(MINOR_DOB).guardianUserId(userB).build()));

        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-14-CN-010 P1.1: chan vong phai dung userId, khong so sanh patientId voi userId")
    void cycleGuardDoesNotConfusePatientIdWithUserId() {
        // Regression for the ID-type bug: the guardian's stored guardianUserId happened to equal
        // the TARGET patient's patientId. Under the buggy comparison that coincidence rejected a
        // perfectly legal link; with the correct users.id comparison it must be allowed.
        UUID patientAId = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID patientBId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientAId))
                .thenReturn(Optional.of(restoredPatient(patientAId, MINOR_DOB, userA, "Nguyen Van Cha", null)));
        when(userRepository.findById(userB))
                .thenReturn(Optional.of(guardianUser(userB, roleId, true)));
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(patientRole(roleId)));
        // B's guardianUserId equals A's PATIENT id, not A's USER id: not a real cycle.
        when(patientRepository.findByUserId(userB))
                .thenReturn(Optional.of(restoredPatient(patientBId, MINOR_DOB, userB, "Nguyen Van Cha", patientAId)));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResult result = service.update(
                patientAId,
                guardianCommand(MINOR_DOB).guardianUserId(userB).build());

        assertEquals(userB, result.guardianUserId());
    }

    @Test
    @DisplayName("NCL-14-CN-010 P1.1: guardian khong co ho so benh nhan thi khong tao vong")
    void guardianWithoutPatientProfileIsAccepted() {
        UUID patientAId = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientAId))
                .thenReturn(Optional.of(restoredPatient(patientAId, MINOR_DOB, userA, "Nguyen Van Cha", null)));
        when(userRepository.findById(userB))
                .thenReturn(Optional.of(guardianUser(userB, roleId, true)));
        when(roleRepository.findByName("PATIENT")).thenReturn(Optional.of(patientRole(roleId)));
        when(patientRepository.findByUserId(userB)).thenReturn(Optional.empty());
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResult result = service.update(
                patientAId,
                guardianCommand(MINOR_DOB).guardianUserId(userB).build());

        assertEquals(userB, result.guardianUserId());
    }

    @Test
    @DisplayName("NCL-14-CN-010 P2.1: khong gan duoc nguoi giam ho cho ho so da thanh nien (QTN-44)")
    void rejectsGuardianAssignmentForAdultPatient() {
        UUID patientId = UUID.randomUUID();
        UUID guardianUserId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientId))
                .thenReturn(Optional.of(restoredPatient(patientId, ADULT_DOB, null, null, null)));

        ValidationException exception = assertThrows(ValidationException.class, () -> service.update(
                patientId,
                guardianCommand(ADULT_DOB).guardianUserId(guardianUserId).build()));

        assertEquals("guardianUserId", exception.getField());
        // The adult check runs first, so no guardian lookup happens at all.
        verify(userRepository, never()).findById(any());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-14-CN-010 P2.1: dung 18 tuoi thi khong con la nguoi chua thanh nien")
    void rejectsGuardianAssignmentWhenExactlyEighteenYearsOld() {
        UUID patientId = UUID.randomUUID();
        UUID guardianUserId = UUID.randomUUID();
        LocalDate exactlyEighteenToday = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .minusYears(18);

        when(patientRepository.findByIdForUpdate(patientId))
                .thenReturn(Optional.of(restoredPatient(patientId, exactlyEighteenToday, null, null, null)));

        assertThrows(ValidationException.class, () -> service.update(
                patientId,
                guardianCommand(exactlyEighteenToday).guardianUserId(guardianUserId).build()));

        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-14-CN-010 P2.1: bo trong guardianUserId tren ho so nguoi lon duoc phep")
    void omittingGuardianUserIdOnAdultPatientIsAllowed() {
        UUID patientId = UUID.randomUUID();
        UUID storedGuardianUserId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientId))
                .thenReturn(Optional.of(restoredPatient(
                        patientId, ADULT_DOB, null, "Nguyen Van Cha", storedGuardianUserId)));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResult result = service.update(patientId, guardianCommand(ADULT_DOB).build());

        assertEquals(storedGuardianUserId, result.guardianUserId());
    }

    @Test
    @DisplayName("NCL-14-CN-010: bo trong guardianUserId thi giu nguyen gia tri dang luu")
    void preservesStoredGuardianUserIdWhenOmitted() {
        UUID patientId = UUID.randomUUID();
        UUID storedGuardianUserId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientId))
                .thenReturn(Optional.of(restoredPatient(
                        patientId, MINOR_DOB, null, "Nguyen Van Cha", storedGuardianUserId)));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResult result = service.update(patientId, guardianCommand(MINOR_DOB).build());

        assertEquals(storedGuardianUserId, result.guardianUserId());
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("NCL-14-CN-010: transitionToAdult xoa guardianUserId")
    void transitionToAdultClearsGuardianUserId() {
        UUID patientId = UUID.randomUUID();
        UUID storedGuardianUserId = UUID.randomUUID();

        when(patientRepository.findByIdForUpdate(patientId))
                .thenReturn(Optional.of(restoredPatient(
                        patientId, ADULT_DOB, null, "Nguyen Van Cha", storedGuardianUserId)));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResult result = service.update(
                patientId,
                guardianCommand(ADULT_DOB).transitionToAdult(true).build());

        org.junit.jupiter.api.Assertions.assertNull(result.guardianUserId());
        org.junit.jupiter.api.Assertions.assertNull(result.guardianName());
    }
}

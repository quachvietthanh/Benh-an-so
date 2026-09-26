package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.exception.PatientConsentRequiredException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.patient.RegisterPatientCommand;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.outbound.generator.PatientCodeGenerator;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterPatientService - Unit Tests (NCL-15-CN-001 / QTN-24)")
class RegisterPatientServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PatientChangeLogRepository patientChangeLogRepository;
    @Mock private PatientCodeGenerator patientCodeGenerator;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private com.benhsoan.port.outbound.repository.patient.PatientConsentHistoryRepository patientConsentHistoryRepository;

    private RegisterPatientService service;
    private final UUID currentUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PatientChangeDetailBuilder changeDetailBuilder = new PatientChangeDetailBuilder(new ObjectMapper());
        PatientResultMapper patientResultMapper = new PatientResultMapper();

        service = new RegisterPatientService(
                patientRepository,
                patientChangeLogRepository,
                patientCodeGenerator,
                currentUserPort,
                changeDetailBuilder,
                patientResultMapper,
                auditLogRepository,
                patientConsentHistoryRepository,
                new ObjectMapper()
        );

        lenient().when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
    }

    @Test
    @DisplayName("TC-01: Đăng ký bệnh nhân thành công khi có consentAgreed = true")
    void registersPatientSuccessfullyWithConsent() {
        when(patientCodeGenerator.generate()).thenReturn("BN000001");
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .identityNumber("079095001234")
                .bloodType(BloodType.O_POSITIVE)
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        PatientResult result = service.register(command);

        assertNotNull(result);
        assertEquals("BN000001", result.patientCode());
        assertEquals("Nguyen Van A", result.fullName());
        assertTrue(result.consentAgreed());
        assertNotNull(result.consentAgreedAt());
        assertEquals("v1.0", result.consentVersion());

        verify(patientRepository).save(any(Patient.class));
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
        verify(auditLogRepository).save(any(AuditLog.class));
        verify(patientConsentHistoryRepository).save(any(com.benhsoan.domain.patient.PatientConsentRecord.class));
    }

    @Test
    @DisplayName("TC-02 / QTN-24: Chặn đăng ký khi consentAgreed = false")
    void rejectsRegistrationWhenConsentIsFalse() {
        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .consentAgreed(false)
                .build();

        assertThrows(PatientConsentRequiredException.class, () -> service.register(command));
    }

    @Test
    @DisplayName("TC-02 / QTN-24: Chặn đăng ký khi consentAgreed = null")
    void rejectsRegistrationWhenConsentIsNull() {
        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .consentAgreed(null)
                .build();

        assertThrows(PatientConsentRequiredException.class, () -> service.register(command));
    }

    @Test
    @DisplayName("QTN-24: Chặn version consent không thuộc danh sách server quản lý")
    void rejectsRegistrationWhenConsentVersionIsUnsupported() {
        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .consentAgreed(true)
                .consentVersion("client-defined-v2")
                .build();

        assertThrows(ValidationException.class, () -> service.register(command));
        verify(patientRepository, never()).save(any(Patient.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Finding 3 Fix: identityNumber rỗng/khoảng trắng được chuẩn hóa thành null và không query trùng lặp")
    void registersPatientWithBlankIdentityNumberNormalizesToNull() {
        when(patientCodeGenerator.generate()).thenReturn("BN000001");
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .identityNumber("   ")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        PatientResult result = service.register(command);

        assertNotNull(result);
        verify(patientRepository, never()).existsByIdentityNumber(any());
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-007 TC-01 & TC-03: Đăng ký bệnh nhân thành công với người liên hệ khẩn cấp đầy đủ và lưu change log")
    void registersPatientWithEmergencyContactSuccessfully() {
        when(patientCodeGenerator.generate()).thenReturn("BN000002");
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("Le Thi B")
                .emergencyRelationship("Vợ")
                .emergencyPhone("+84909998877")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        PatientResult result = service.register(command);

        assertNotNull(result);
        assertEquals("Le Thi B", result.emergencyContact());
        assertEquals("Vợ", result.emergencyRelationship());
        assertEquals("0909998877", result.emergencyPhone());

        verify(patientRepository).save(any(Patient.class));
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
    }

    @Test
    @DisplayName("NCL-02-CN-007 TC-02: Từ chối đăng ký khi số điện thoại người liên hệ khẩn cấp sai định dạng")
    void rejectsRegistrationWhenEmergencyPhoneIsInvalid() {
        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("Le Thi B")
                .emergencyRelationship("Vợ")
                .emergencyPhone("01234567890")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.register(command));
        assertTrue(ex.getMessage().contains("emergencyPhone"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-007 Cohesive Triplet: Từ chối khi thiếu quan hệ nhân thân nhưng có họ tên và SĐT người liên hệ khẩn cấp")
    void rejectsRegistrationWhenEmergencyRelationshipIsMissing() {
        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van A")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.MALE)
                .phone("0909000001")
                .emergencyContact("Le Thi B")
                .emergencyRelationship("   ")
                .emergencyPhone("0909998877")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.register(command));
        assertTrue(ex.getMessage().contains("emergencyRelationship"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-01 & TC-03: Đăng ký bệnh nhân trẻ em thành công với người giám hộ hợp lệ, consent do người giám hộ đứng tên")
    void registersPediatricPatientWithGuardianSuccessfully() {
        when(patientCodeGenerator.generate()).thenReturn("BN000003");
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van Con")
                .dateOfBirth(LocalDate.now().minusYears(5))
                .gender(Gender.MALE)
                .guardianName("Nguyen Van Bo")
                .guardianRelationship("Bố")
                .guardianPhone("0912345678")
                .guardianIdentityNumber("001200000001")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        PatientResult result = service.register(command);

        assertNotNull(result);
        assertEquals("BN000003", result.patientCode());
        assertEquals("Nguyen Van Con", result.fullName());
        assertTrue(result.isMinor());
        org.junit.jupiter.api.Assertions.assertFalse(result.requiresAdultTransitionPrompt());
        assertEquals("Nguyen Van Bo", result.guardianName());
        assertEquals("Bố", result.guardianRelationship());
        assertEquals("0912345678", result.guardianPhone());
        assertEquals("001200000001", result.guardianIdentityNumber());
        assertEquals("Nguyen Van Bo", result.consentSignerName(), "TC-03: Phiếu đồng ý xử lý dữ liệu cá nhân phải do người giám hộ đứng tên");
        assertTrue(result.consentAgreed());

        verify(patientRepository).save(any(Patient.class));
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-02: Từ chối đăng ký bệnh nhân trẻ em khi thiếu họ tên người giám hộ")
    void rejectsPediatricPatientWhenGuardianNameIsMissing() {
        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van Con")
                .dateOfBirth(LocalDate.now().minusYears(7))
                .gender(Gender.MALE)
                .guardianRelationship("Mẹ")
                .guardianPhone("0912345678")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.register(command));
        assertTrue(ex.getMessage().contains("guardianName"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-02: Từ chối đăng ký bệnh nhân trẻ em khi thiếu mối quan hệ với người giám hộ")
    void rejectsPediatricPatientWhenGuardianRelationshipIsMissing() {
        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van Con")
                .dateOfBirth(LocalDate.now().minusYears(7))
                .gender(Gender.MALE)
                .guardianName("Nguyen Thi Me")
                .guardianPhone("0912345678")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.register(command));
        assertTrue(ex.getMessage().contains("guardianRelationship"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-02: Từ chối đăng ký bệnh nhân trẻ em khi thiếu số điện thoại người giám hộ")
    void rejectsPediatricPatientWhenGuardianPhoneIsMissing() {
        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van Con")
                .dateOfBirth(LocalDate.now().minusYears(7))
                .gender(Gender.MALE)
                .guardianName("Nguyen Thi Me")
                .guardianRelationship("Mẹ")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.register(command));
        assertTrue(ex.getMessage().contains("guardianPhone"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-008 TC-02: Từ chối đăng ký bệnh nhân trẻ em khi số điện thoại người giám hộ sai định dạng")
    void rejectsPediatricPatientWhenGuardianPhoneIsInvalid() {
        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van Con")
                .dateOfBirth(LocalDate.now().minusYears(7))
                .gender(Gender.MALE)
                .guardianName("Nguyen Thi Me")
                .guardianRelationship("Mẹ")
                .guardianPhone("012345")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.register(command));
        assertTrue(ex.getMessage().contains("guardianPhone"));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("NCL-02-CN-008: Bệnh nhân người lớn đăng ký không cần người giám hộ, consent do chính bệnh nhân đứng tên")
    void registersAdultPatientWithoutGuardianConsentSignedBySelf() {
        when(patientCodeGenerator.generate()).thenReturn("BN000004");
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Tran Van Lon")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.FEMALE)
                .phone("0988776655")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        PatientResult result = service.register(command);

        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertFalse(result.isMinor());
        org.junit.jupiter.api.Assertions.assertFalse(result.requiresAdultTransitionPrompt());
        org.junit.jupiter.api.Assertions.assertNull(result.guardianName());
        assertEquals("Tran Van Lon", result.consentSignerName(), "Người lớn tự đứng tên phiếu đồng ý");
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("P1-2 / QTN-44: Từ chối đăng ký bệnh nhân trẻ em khi consentSignerName khác người giám hộ")
    void rejectsPediatricPatientWhenConsentSignerMismatchGuardian() {
        RegisterPatientCommand command = RegisterPatientCommand.builder()
                .fullName("Nguyen Van Con")
                .dateOfBirth(LocalDate.now().minusYears(7))
                .gender(Gender.MALE)
                .guardianName("Nguyen Thi Me")
                .guardianRelationship("Mẹ")
                .guardianPhone("0912345678")
                .consentSignerName("Người Khác")
                .consentAgreed(true)
                .consentVersion("v1.0")
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.register(command));
        assertEquals("consentSignerName", ex.getField());
        verify(patientRepository, never()).save(any(Patient.class));
    }
}

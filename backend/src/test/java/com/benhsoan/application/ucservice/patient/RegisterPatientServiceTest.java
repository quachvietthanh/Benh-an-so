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
                auditLogRepository
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
}

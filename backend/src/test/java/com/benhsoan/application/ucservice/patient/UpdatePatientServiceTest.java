package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
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
import com.benhsoan.port.dto.command.patient.UpdatePatientCommand;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePatientService - Unit Tests (NCL-15-CN-001 / TC-03, TC-04)")
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

        when(currentUserPort.getCurrentUserId()).thenReturn(currentUserId);
    }

    @Test
    @DisplayName("TC-03: Ghi nhận rút lại sự đồng ý, cập nhật nonMedicalUseRestricted = true")
    void withdrawsConsentSuccessfully() {
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

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existing));
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
        verify(patientChangeLogRepository).save(any(PatientChangeLog.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }
}

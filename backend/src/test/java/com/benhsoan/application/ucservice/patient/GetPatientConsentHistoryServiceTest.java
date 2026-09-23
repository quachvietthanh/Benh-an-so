package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientConsentRecord;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.ConsentHistoryStatus;
import com.benhsoan.domain.patient.enums.ConsentScope;
import com.benhsoan.domain.patient.exception.PatientConsentAccessDeniedException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.result.patient.PatientConsentHistoryResult;
import com.benhsoan.port.outbound.repository.patient.PatientConsentHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPatientConsentHistoryService Unit Tests (NCL-15-CN-005 / AC-02)")
class GetPatientConsentHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");

    @Mock private PatientRepository patientRepository;
    @Mock private PatientConsentHistoryRepository patientConsentHistoryRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private PatientAccessGuard patientAccessGuard;

    private GetPatientConsentHistoryService service;
    private final UUID currentUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GetPatientConsentHistoryService(
                patientRepository,
                patientConsentHistoryRepository,
                currentUserPort,
                patientAccessGuard
        );
    }

    private Patient createTestPatient(UUID patientId) {
        return Patient.restore(
                patientId,
                "PAT-2026-0001",
                "Trần Thị Hoa",
                LocalDate.of(1995, 3, 15),
                Gender.FEMALE,
                "0987654321",
                "hoa@example.com",
                "456 Cầu Giấy, Hà Nội",
                "001095000002",
                "DN4010987654321",
                BloodType.A_POSITIVE,
                null, null, null, null, null, null, null, null,
                "Trần Thị Hoa",
                true,
                NOW.minusSeconds(86400 * 30),
                NOW.minusSeconds(86400 * 30),
                null,
                currentUserId,
                true,
                NOW.minusSeconds(86400 * 30),
                "v1.0",
                false,
                null,
                null,
                false
        );
    }

    @Test
    @DisplayName("AC-02: Tra cứu danh sách lịch sử phiên bản theo thứ tự thời gian")
    void getConsentHistory_returnsRecordsFromRepository() {
        UUID patientId = UUID.randomUUID();
        Patient patient = createTestPatient(patientId);

        when(currentUserPort.hasPermission("PATIENT_READ")).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        PatientConsentRecord r1 = PatientConsentRecord.restore(
                UUID.randomUUID(),
                patientId,
                1,
                "v1.0",
                ConsentHistoryStatus.AGREED,
                ConsentScope.defaultAll(),
                true,
                NOW.minusSeconds(86400 * 10),
                false,
                null,
                null,
                false,
                "Trần Thị Hoa",
                currentUserId,
                NOW.minusSeconds(86400 * 10)
        );

        PatientConsentRecord r2 = PatientConsentRecord.restore(
                UUID.randomUUID(),
                patientId,
                2,
                "v1.0",
                ConsentHistoryStatus.PARTIALLY_WITHDRAWN,
                EnumSet.of(ConsentScope.TREATMENT),
                true,
                NOW.minusSeconds(86400 * 10),
                false,
                null,
                null,
                true,
                "Trần Thị Hoa",
                currentUserId,
                NOW
        );

        when(patientConsentHistoryRepository.findByPatientId(patientId)).thenReturn(List.of(r1, r2));

        List<PatientConsentHistoryResult> results = service.getConsentHistory(patientId);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(1, results.get(0).versionNumber());
        assertEquals(ConsentHistoryStatus.AGREED, results.get(0).status());
        assertEquals(2, results.get(1).versionNumber());
        assertEquals(ConsentHistoryStatus.PARTIALLY_WITHDRAWN, results.get(1).status());
        assertTrue(results.get(1).nonMedicalUseRestricted());
    }

    @Test
    @DisplayName("AC-02: Tự động fallback bản ghi mặc định khi lịch sử rỗng nhưng patient có consent phẳng")
    void getConsentHistory_emptyHistory_createsFallbackRecord() {
        UUID patientId = UUID.randomUUID();
        Patient patient = createTestPatient(patientId);

        when(currentUserPort.hasPermission("PATIENT_READ")).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(patientConsentHistoryRepository.findByPatientId(patientId)).thenReturn(Collections.emptyList());

        List<PatientConsentHistoryResult> results = service.getConsentHistory(patientId);

        assertNotNull(results);
        assertEquals(1, results.size());
        PatientConsentHistoryResult fallback = results.get(0);
        assertEquals(1, fallback.versionNumber());
        assertEquals(ConsentHistoryStatus.AGREED, fallback.status());
        assertTrue(fallback.consentAgreed());
        assertFalse(fallback.consentWithdrawn());
    }

    @Test
    @DisplayName("Security: Từ chối truy cập khi không có quyền staff và không sở hữu hồ sơ")
    void unauthorizedAccess_throwsPatientConsentAccessDeniedException() {
        UUID patientId = UUID.randomUUID();

        when(currentUserPort.hasPermission("PATIENT_READ")).thenReturn(false);
        when(currentUserPort.hasPermission("PATIENT_CONSENT_UPDATE")).thenReturn(false);
        doThrow(new AccessDeniedException("Access denied")).when(patientAccessGuard).requirePatientOwnership(patientId);

        assertThrows(PatientConsentAccessDeniedException.class, () -> service.getConsentHistory(patientId));
    }

    @Test
    @DisplayName("Edge Case: Ném PatientNotFoundException khi mã bệnh nhân không tồn tại")
    void patientNotFound_throwsException() {
        UUID patientId = UUID.randomUUID();

        when(currentUserPort.hasPermission("PATIENT_READ")).thenReturn(true);
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.getConsentHistory(patientId));
    }
}

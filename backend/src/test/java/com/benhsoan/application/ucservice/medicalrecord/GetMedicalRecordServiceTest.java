package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMedicalRecordService - Unit Tests")
class GetMedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock
    private MedicalRecordAuthorizationService authorizationService;
    @Mock
    private MedicalRecordAccessAuditService accessAuditService;
    @Mock
    private MedicalRecordTemplateApplicationMapper templateApplicationMapper;
    @Mock
    private MedicalRecordResultMapper resultMapper;
    @Mock
    private ClockPort clockPort;

    @InjectMocks
    private GetMedicalRecordService service;

    private final UUID recordId = UUID.randomUUID();
    private final UUID visitId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-08-11T13:00:00Z");

    private MedicalRecord record() {
        return MedicalRecord.restore(
                recordId, visitId, "Headache", "Pain", "None", "Normal", "Stable", "Rest",
                "Follow-up", "Migraine", MedicalRecordStatus.OPEN,
                null, null, doctorId, now, null, null
        );
    }

    private Visit visit() {
        return Visit.restore(
                visitId, "VS-0001", patientId, doctorId, null, null,
                VisitType.WALK_IN, VisitStatus.COMPLETED, now, now, now,
                "Exam", null, doctorId, now, now
        );
    }

    private Patient patient() {
        return Patient.restore(
                patientId, "BN-0001", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                Gender.MALE, "0900000000", "patient@email.com", "Hanoi", "ID-1", "BH-1",
                com.benhsoan.domain.patient.enums.BloodType.UNKNOWN,
                "Contact", "0911111111", true, now, now, doctorId, doctorId
        );
    }

    private User doctor() {
        return User.restore(
                doctorId, "doctor", "hash", "Dr. Tran B", "doctor@hospital.com", "0988888888",
                UUID.randomUUID(), true, now, now
        );
    }

    private MedicalRecordDiagnosis diagnosis() {
        return MedicalRecordDiagnosis.restore(
                UUID.randomUUID(), recordId, null, "G43", "Migraine",
                DiagnosisType.PRIMARY, null, doctorId, now, now, now);
    }

    private MedicalRecordDetailResult detailResult() {
        return new MedicalRecordDetailResult(
                new MedicalRecordDetailResult.PatientInfo(patientId, "BN-0001", "Nguyen Van A",
                        LocalDate.of(1990, 1, 1), Gender.MALE, "0900000000", "ID-1", "BH-1"),
                new MedicalRecordDetailResult.VisitInfo(visitId, "VS-0001", VisitType.WALK_IN,
                        VisitStatus.COMPLETED, now, now, now, "Exam", null, doctorId, "Dr. Tran B"),
                recordId, "Headache", "Pain", "None", "Normal", "Stable", "Rest",
                "Follow-up", "Migraine", MedicalRecordStatus.OPEN, null, null, null, null, null,
                "G43", "Migraine", List.of(), List.of());
    }

    @Test
    @DisplayName("getDetailByVisitId - loads detail and records read access audit log (QTN-02)")
    void getDetailByVisitIdWritesAuditLogAndReturnsDetail() {
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record()));
        when(authorizationService.requireReadAccess()).thenReturn(userId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit()));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient()));
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor()));
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(recordId)).thenReturn(List.of(diagnosis()));
        when(clockPort.now()).thenReturn(now);
        when(resultMapper.toDetailResult(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(detailResult());

        MedicalRecordDetailResult result = service.getDetailByVisitId(visitId);

        assertNotNull(result);
        assertEquals(recordId, result.medicalRecordId());
        assertEquals("Headache", result.chiefComplaint());
        assertEquals("BN-0001", result.patient().patientCode());
        assertEquals("VS-0001", result.visit().visitCode());
        assertEquals("Dr. Tran B", result.visit().doctorName());

        verify(accessAuditService).recordRecordView(patientId, visitId, recordId, userId, now);
    }

    @Test
    @DisplayName("getDetailByVisitId - throws MedicalRecordNotFoundException when record not found")
    void getDetailByVisitIdThrowsWhenNotFound() {
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.empty());

        assertThrows(MedicalRecordNotFoundException.class, () -> service.getDetailByVisitId(visitId));
        verifyNoInteractions(accessAuditService);
    }

    @Test
    @DisplayName("getHistoryByPatientId - returns history list ordered by visit date and writes audit log")
    void getHistoryByPatientIdReturnsListAndWritesAuditLog() {
        when(authorizationService.requireReadAccess()).thenReturn(userId);
        when(visitRepository.findByPatientIdOrderByVisitAtDesc(patientId)).thenReturn(List.of(visit()));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit()));
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record()));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient()));
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor()));
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(recordId)).thenReturn(List.of(diagnosis()));
        when(clockPort.now()).thenReturn(now);
        when(resultMapper.toDetailResult(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(detailResult());

        List<MedicalRecordDetailResult> history = service.getHistoryByPatientId(patientId);

        assertEquals(1, history.size());
        assertEquals(recordId, history.get(0).medicalRecordId());

        verify(accessAuditService).recordHistoryView(patientId, userId, now);
    }

    @Test
    @DisplayName("getHistoryByPatientId - returns empty list when patient has no visits")
    void getHistoryByPatientIdReturnsEmptyList() {
        when(authorizationService.requireReadAccess()).thenReturn(userId);
        when(visitRepository.findByPatientIdOrderByVisitAtDesc(patientId)).thenReturn(List.of());
        when(clockPort.now()).thenReturn(now);

        List<MedicalRecordDetailResult> history = service.getHistoryByPatientId(patientId);

        assertTrue(history.isEmpty());
        verify(accessAuditService).recordHistoryView(patientId, userId, now);
    }
}

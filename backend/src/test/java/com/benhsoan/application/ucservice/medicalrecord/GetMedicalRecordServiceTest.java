package com.benhsoan.application.ucservice.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.enums.DiagnosisType;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult;
import com.benhsoan.port.outbound.repository.crudRepository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.crudRepository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMedicalRecordService - Unit Tests")
class GetMedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    @Mock
    private VisitRepository visitRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MedicalRecordAuthorizationService authorizationService;
    @Mock
    private MedicalRecordAccessAuditService accessAuditService;
    @Mock
    private MedicalRecordResultMapper resultMapper;
    @Mock
    private ClockPort clockPort;

    private GetMedicalRecordService service;

    private final UUID visitId = UUID.randomUUID();
    private final UUID recordId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        service = new GetMedicalRecordService(
                medicalRecordRepository, medicalRecordDiagnosisRepository, visitRepository,
                patientRepository, userRepository, authorizationService, accessAuditService,
                resultMapper, clockPort);
    }

    private MedicalRecord record() {
        return MedicalRecord.restore(
                recordId, visitId, "Headache", "Pain", "None", "Normal",
                "Stable", "Rest", "Follow-up", "Migraine", MedicalRecordStatus.OPEN,
                null, null, userId, now, null, now);
    }

    private Visit visit() {
        return Visit.restore(
                visitId, "VS-0001", patientId, doctorId, null, null,
VisitType.WALK_IN, VisitStatus.COMPLETED, now, now, now,
                "Exam", null, userId, now, now);
    }

    private Patient patient() {
        return Patient.restore(
                patientId, "BN-0001", "Nguyen Van A", LocalDate.of(1990, 1, 1),
                Gender.MALE, "0900000000", null, "Hanoi", "ID-1", "BH-1",
                null, null, null, true, now, now, null, userId);
    }

    private User doctor() {
        return User.restore(
                doctorId, "doctor1", "hash", "Dr. Tran B", "dr@hospital.vn",
                null, UUID.randomUUID(), true, now, now);
    }

    private MedicalRecordDiagnosis primaryDiagnosis() {
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
                "Follow-up", "Migraine", MedicalRecordStatus.OPEN, null, null,
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
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(recordId))
                .thenReturn(List.of(primaryDiagnosis()));
        when(clockPort.now()).thenReturn(now);
        when(resultMapper.toDetailResult(any(), any(), any(), any(), any()))
                .thenReturn(detailResult());

        MedicalRecordDetailResult result = service.getDetailByVisitId(visitId);

        assertEquals(detailResult(), result);
        verify(accessAuditService).recordRecordView(patientId, visitId, recordId, userId, now);
    }

    @Test
    @DisplayName("getDetailByVisitId - throws when record does not exist")
    void getDetailByVisitIdThrowsWhenRecordMissing() {
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.empty());

        assertThrows(MedicalRecordNotFoundException.class,
                () -> service.getDetailByVisitId(visitId));
        verify(accessAuditService, never()).recordRecordView(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getHistoryByPatientId - writes history audit and returns only visits that have a record")
    void getHistoryByPatientIdReturnsExistingRecordsOnly() {
        Visit visitWithoutRecord = Visit.restore(
                UUID.randomUUID(), "VS-0002", patientId, doctorId, null, null,
VisitType.WALK_IN, VisitStatus.WAITING, now, null, null,
                "Exam 2", null, userId, now, null);

        when(authorizationService.requireReadAccess()).thenReturn(userId);
        when(clockPort.now()).thenReturn(now);
        when(visitRepository.findByPatientIdOrderByVisitAtDesc(patientId))
                .thenReturn(List.of(visit(), visitWithoutRecord));
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record()));
        when(medicalRecordRepository.findByVisitId(visitWithoutRecord.getId()))
                .thenReturn(Optional.empty());
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit()));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient()));
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor()));
        when(medicalRecordDiagnosisRepository.findByMedicalRecordId(recordId))
                .thenReturn(List.of(primaryDiagnosis()));
        when(resultMapper.toDetailResult(any(), any(), any(), any(), any()))
                .thenReturn(detailResult());

        List<MedicalRecordDetailResult> history = service.getHistoryByPatientId(patientId);

        assertEquals(1, history.size());
        verify(accessAuditService).recordHistoryView(patientId, userId, now);
        verify(accessAuditService).recordRecordView(patientId, visitId, recordId, userId, now);
    }
}

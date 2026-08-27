package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

@ExtendWith(MockitoExtension.class)
class GetPatientMedicalHistoryServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private MedicalRecordDiagnosisRepository diagnosisRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private CurrentUserPort currentUserPort;

    private GetPatientMedicalHistoryService service;

    @BeforeEach
    void setUp() {
        service = new GetPatientMedicalHistoryService(
                patientRepository,
                visitRepository,
                medicalRecordRepository,
                diagnosisRepository,
                prescriptionRepository,
                userRepository,
                specialtyRepository,
                currentUserPort
        );
    }

    @Test
    void returnsOnlySignedVisitsWithSummary() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID specialtyId = UUID.randomUUID();
        UUID signedVisitId = UUID.randomUUID();
        UUID draftVisitId = UUID.randomUUID();
        UUID signedRecordId = UUID.randomUUID();

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

        Visit signedVisit = mock(Visit.class);
        when(signedVisit.isCompleted()).thenReturn(true);
        when(signedVisit.getId()).thenReturn(signedVisitId);
        when(signedVisit.getDoctorId()).thenReturn(doctorId);
        when(signedVisit.getSpecialtyId()).thenReturn(specialtyId);
        when(signedVisit.getVisitAt()).thenReturn(Instant.parse("2099-01-10T02:00:00Z"));

        Visit draftVisit = mock(Visit.class);
        when(draftVisit.isCompleted()).thenReturn(true);
        when(draftVisit.getId()).thenReturn(draftVisitId);

        when(visitRepository.findByPatientIdOrderByVisitAtDesc(patientId))
                .thenReturn(List.of(signedVisit, draftVisit));

        MedicalRecord signedRecord = mock(MedicalRecord.class);
        when(signedRecord.isContentLocked()).thenReturn(true);
        when(signedRecord.getId()).thenReturn(signedRecordId);

        MedicalRecord draftRecord = mock(MedicalRecord.class);
        when(draftRecord.isContentLocked()).thenReturn(false);

        when(medicalRecordRepository.findByVisitId(signedVisitId)).thenReturn(Optional.of(signedRecord));
        when(medicalRecordRepository.findByVisitId(draftVisitId)).thenReturn(Optional.of(draftRecord));

        MedicalRecordDiagnosis d1 = mock(MedicalRecordDiagnosis.class);
        when(d1.getDiagnosisName()).thenReturn("Hypertension");
        MedicalRecordDiagnosis d2 = mock(MedicalRecordDiagnosis.class);
        when(d2.getDiagnosisName()).thenReturn("Diabetes");
        when(diagnosisRepository.findByMedicalRecordId(signedRecordId)).thenReturn(List.of(d1, d2));

        when(prescriptionRepository.findByMedicalRecordId(signedRecordId)).thenReturn(List.of(mock(), mock()));

        User doctor = mock(User.class);
        when(doctor.getFullName()).thenReturn("Dr. A");
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        Specialty specialty = mock(Specialty.class);
        when(specialty.getName()).thenReturn("Internal Medicine");
        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));

        var results = service.getMedicalHistory();

        assertEquals(1, results.size());
        var summary = results.get(0);
        assertEquals(signedVisitId, summary.visitId());
        assertEquals("Dr. A", summary.doctorName());
        assertEquals("Internal Medicine", summary.specialtyName());
        assertEquals("Hypertension, Diabetes", summary.diagnosisSummary());
        assertEquals(2, summary.prescriptionCount());
    }

    @Test
    void returnsEmptyListWhenNoVisits() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));
        when(visitRepository.findByPatientIdOrderByVisitAtDesc(patientId)).thenReturn(List.of());

        assertEquals(List.of(), service.getMedicalHistory());
    }

    @Test
    void excludesUnsignedRecords() {
        UUID userId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID openVisitId = UUID.randomUUID();

        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(patientId);
        when(currentUserPort.getCurrentUserId()).thenReturn(userId);
        when(patientRepository.findByUserId(userId)).thenReturn(Optional.of(patient));

        Visit openVisit = mock(Visit.class);
        when(openVisit.isCompleted()).thenReturn(true);
        when(openVisit.getId()).thenReturn(openVisitId);
        when(visitRepository.findByPatientIdOrderByVisitAtDesc(patientId)).thenReturn(List.of(openVisit));

        MedicalRecord openRecord = mock(MedicalRecord.class);
        when(openRecord.isContentLocked()).thenReturn(false);
        when(medicalRecordRepository.findByVisitId(openVisitId)).thenReturn(Optional.of(openRecord));

        assertEquals(List.of(), service.getMedicalHistory());
    }

    @Test
    void rejectsWhenNoPatientProfileIsLinked() {
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(patientRepository.findByUserId(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> service.getMedicalHistory());
    }
}

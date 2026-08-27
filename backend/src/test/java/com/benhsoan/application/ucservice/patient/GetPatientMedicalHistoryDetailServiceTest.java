package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class GetPatientMedicalHistoryDetailServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");

    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private MedicalRecordDiagnosisRepository diagnosisRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PrescriptionItemRepository prescriptionItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private PatientAccessGuard patientAccessGuard;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserPort currentUserPort;
    @Mock private ClockPort clockPort;

    private GetPatientMedicalHistoryDetailService service;

    @BeforeEach
    void setUp() {
        service = new GetPatientMedicalHistoryDetailService(
                visitRepository,
                medicalRecordRepository,
                diagnosisRepository,
                prescriptionRepository,
                prescriptionItemRepository,
                userRepository,
                specialtyRepository,
                patientAccessGuard,
                auditLogRepository,
                currentUserPort,
                clockPort,
                new ObjectMapper()
        );
    }

    @Test
    void returnsDetailForSignedRecordAndWritesReadAudit() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID specialtyId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Visit visit = mock(Visit.class);
        when(visit.getId()).thenReturn(visitId);
        when(visit.getPatientId()).thenReturn(patientId);
        when(visit.getDoctorId()).thenReturn(doctorId);
        when(visit.getSpecialtyId()).thenReturn(specialtyId);
        when(visit.getVisitAt()).thenReturn(Instant.parse("2099-01-10T02:00:00Z"));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        when(patientAccessGuard.requirePatientOwnership(patientId, ResourceType.MEDICAL_RECORD, visitId))
                .thenReturn(mock(com.benhsoan.domain.patient.Patient.class));

        MedicalRecord record = mock(MedicalRecord.class);
        when(record.isContentLocked()).thenReturn(true);
        when(record.getId()).thenReturn(recordId);
        when(record.getDoctorInstructions()).thenReturn("Rest and hydrate");
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record));

        MedicalRecordDiagnosis d1 = mock(MedicalRecordDiagnosis.class);
        when(d1.getDiagnosisCode()).thenReturn("I10");
        when(d1.getDiagnosisName()).thenReturn("Essential hypertension");
        when(diagnosisRepository.findByMedicalRecordId(recordId)).thenReturn(List.of(d1));

        Prescription prescription = mock(Prescription.class);
        when(prescription.getId()).thenReturn(prescriptionId);
        when(prescriptionRepository.findByMedicalRecordId(recordId)).thenReturn(List.of(prescription));

        PrescriptionItem item = mock(PrescriptionItem.class);
        when(item.getMedicineName()).thenReturn("Paracetamol");
        when(item.getQuantity()).thenReturn(10);
        when(item.getDosage()).thenReturn("500mg");
        when(item.getInstructions()).thenReturn("After meals");
        when(prescriptionItemRepository.findByPrescriptionId(prescriptionId)).thenReturn(List.of(item));

        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);

        User doctor = mock(User.class);
        when(doctor.getFullName()).thenReturn("Dr. A");
        when(userRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        Specialty specialty = mock(Specialty.class);
        when(specialty.getName()).thenReturn("Internal Medicine");
        when(specialtyRepository.findById(specialtyId)).thenReturn(Optional.of(specialty));

        var result = service.getMedicalHistoryDetail(visitId);

        assertEquals(visitId, result.visitId());
        assertEquals("Dr. A", result.doctorName());
        assertEquals("Internal Medicine", result.specialtyName());
        assertEquals("Rest and hydrate", result.doctorAdvice());
        assertEquals(1, result.diagnoses().size());
        assertEquals("I10", result.diagnoses().get(0).icd10Code());
        assertEquals(1, result.prescriptionItems().size());
        assertEquals("Paracetamol", result.prescriptionItems().get(0).medicineName());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(ActionType.READ, audit.getActionType());
        assertEquals(ResourceType.MEDICAL_RECORD, audit.getResourceType());
        assertEquals(recordId, audit.getResourceId());
        assertEquals(actorId, audit.getUserId());
    }

    @Test
    void rejectsCrossPatientAccessWithForbidden() {
        UUID visitId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();

        Visit visit = mock(Visit.class);
        when(visit.getPatientId()).thenReturn(otherPatientId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        when(patientAccessGuard.requirePatientOwnership(otherPatientId, ResourceType.MEDICAL_RECORD, visitId))
                .thenThrow(new AccessDeniedException("Patient may only access their own data."));

        assertThrows(AccessDeniedException.class, () -> service.getMedicalHistoryDetail(visitId));
        verify(patientAccessGuard).requirePatientOwnership(otherPatientId, ResourceType.MEDICAL_RECORD, visitId);
    }

    @Test
    void hidesUnsignedRecord() {
        UUID visitId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        Visit visit = mock(Visit.class);
        when(visit.getPatientId()).thenReturn(patientId);
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        when(patientAccessGuard.requirePatientOwnership(patientId, ResourceType.MEDICAL_RECORD, visitId))
                .thenReturn(mock(com.benhsoan.domain.patient.Patient.class));

        MedicalRecord draft = mock(MedicalRecord.class);
        when(draft.isContentLocked()).thenReturn(false);
        when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(draft));

        assertThrows(MedicalRecordNotFoundException.class, () -> service.getMedicalHistoryDetail(visitId));
    }
}

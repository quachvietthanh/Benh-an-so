package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordStatus;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionInterconnectionLog;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionInvalidStatusException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.outbound.interconnection.PrescriptionInterconnectionGatewayPort;
import com.benhsoan.port.outbound.interconnection.PrescriptionInterconnectionGatewayResponse;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionInterconnectionLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class SendPrescriptionInterconnectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T03:00:00Z");
    private static final UUID DOCTOR_ID = UUID.randomUUID();

    private final PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
    private final PrescriptionInterconnectionLogRepository logRepository = mock(PrescriptionInterconnectionLogRepository.class);
    private final PrescriptionInterconnectionGatewayPort gatewayPort = mock(PrescriptionInterconnectionGatewayPort.class);
    private final MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final ClinicConfigurationRepository clinicConfigurationRepository = mock(ClinicConfigurationRepository.class);
    private final PrescriptionDisplayContextResolver displayContextResolver = mock(PrescriptionDisplayContextResolver.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);

    private SendPrescriptionInterconnectionService service;

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(logRepository.findByPrescriptionId(any())).thenReturn(List.of());
        when(prescriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(ClinicConfiguration.create(
                "Phong kham A", "1 Duong A", "0900000000", LocalTime.of(8, 0), LocalTime.of(17, 0), NOW)));
        when(displayContextResolver.resolve(any(), any())).thenReturn(
                new PrescriptionDisplayContextResolver.PrescriptionDisplayContext(
                        UUID.randomUUID(), "VISIT-001", PATIENT_ID, "PAT-001", "Nguyen Van A", "Dr. B"));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient()));
        service = new SendPrescriptionInterconnectionService(
                prescriptionRepository, logRepository, gatewayPort, medicalRecordRepository, visitRepository,
                patientRepository, clinicConfigurationRepository, displayContextResolver, currentUserPort,
                clockPort, auditLogRepository, new ObjectMapper().findAndRegisterModules());
    }

    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Test
    void sendsResponsibleDoctorsPrescriptionAndWritesSuccessHistoryAndAudit() {
        Prescription prescription = prescription(PrescriptionStatus.PENDING_DISPENSE);
        stubAuthorizedContext(prescription);
        when(gatewayPort.submit(any())).thenReturn(
                new PrescriptionInterconnectionGatewayResponse("LT-20260821-000001", "ACCEPTED", NOW));

        var result = service.send(prescription.getId());

        assertEquals(InterconnectionStatus.SUCCESS, result.status());
        assertEquals("LT-20260821-000001", result.receiptCode());
        assertEquals(InterconnectionStatus.SUCCESS, prescription.getInterconnectionStatus());
        verify(logRepository).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getOutcome().name().equals("SUCCESS") && log.getAttemptNumber() == 1));
        verify(auditLogRepository).save(org.mockito.ArgumentMatchers.argThat(audit ->
                audit.getActionType().name().equals("SEND") && audit.getDetail().contains("SUCCESS")));
    }

    @Test
    void recordsFailedAttemptWhenGatewayRejectsOrDoesNotRespond() {
        Prescription prescription = prescription(PrescriptionStatus.PENDING_DISPENSE);
        stubAuthorizedContext(prescription);
        when(gatewayPort.submit(any())).thenThrow(new RuntimeException("Mock gateway did not respond."));

        var result = service.send(prescription.getId());

        assertEquals(InterconnectionStatus.FAILED, result.status());
        assertEquals("Mock gateway did not respond.", result.failureReason());
        verify(logRepository).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getOutcome().name().equals("FAILED") && log.getFailureReason().contains("did not respond")));
        verify(auditLogRepository).save(org.mockito.ArgumentMatchers.argThat(audit ->
                audit.getDetail().contains("FAILED") && audit.getDetail().contains("did not respond")));
    }

    @Test
    void rejectsDoctorWhoDoesNotManageTheVisitBeforeCallingGateway() {
        Prescription prescription = prescription(PrescriptionStatus.PENDING_DISPENSE);
        when(prescriptionRepository.findByIdForUpdate(prescription.getId())).thenReturn(Optional.of(prescription));
        when(medicalRecordRepository.findById(prescription.getMedicalRecordId()))
                .thenReturn(Optional.of(medicalRecord(UUID.randomUUID())));
        when(visitRepository.findById(any())).thenReturn(Optional.of(visit(UUID.randomUUID())));

        assertThrows(AccessDeniedException.class, () -> service.send(prescription.getId()));

        verify(gatewayPort, never()).submit(any());
        verify(logRepository, never()).save(any(PrescriptionInterconnectionLog.class));
    }

    @Test
    void rejectsCancelledAndAlreadySuccessfulPrescriptionsBeforeCreatingAttempt() {
        Prescription cancelled = prescription(PrescriptionStatus.CANCELLED);
        when(prescriptionRepository.findByIdForUpdate(cancelled.getId())).thenReturn(Optional.of(cancelled));
        assertThrows(ValidationException.class, () -> service.send(cancelled.getId()));

        Prescription succeeded = succeededPrescription();
        when(prescriptionRepository.findByIdForUpdate(succeeded.getId())).thenReturn(Optional.of(succeeded));
        assertThrows(PrescriptionInvalidStatusException.class, () -> service.send(succeeded.getId()));
        verify(gatewayPort, never()).submit(any());
    }

    private void stubAuthorizedContext(Prescription prescription) {
        UUID visitId = UUID.randomUUID();
        when(prescriptionRepository.findByIdForUpdate(prescription.getId())).thenReturn(Optional.of(prescription));
        when(medicalRecordRepository.findById(prescription.getMedicalRecordId()))
                .thenReturn(Optional.of(medicalRecord(visitId)));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit(DOCTOR_ID)));
    }

    private Prescription prescription(PrescriptionStatus status) {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionItem item = PrescriptionItem.restore(UUID.randomUUID(), prescriptionId, UUID.randomUUID(),
                "Paracetamol", "Paracetamol", "500 mg", "vien", "1 vien", 2,
                AdministrationRoute.ORAL, 5, 10, "Sau an", NOW.minusSeconds(300), null);
        return Prescription.restore(prescriptionId, "RX000001", UUID.randomUUID(), status, null, DOCTOR_ID,
                NOW.minusSeconds(300), null, null, List.of(item));
    }

    private Prescription succeededPrescription() {
        Prescription prescription = prescription(PrescriptionStatus.PENDING_DISPENSE);
        prescription.markInterconnectionSucceeded("LT-20260821-000002", NOW.minusSeconds(30));
        return prescription;
    }

    private MedicalRecord medicalRecord(UUID visitId) {
        return MedicalRecord.restore(UUID.randomUUID(), visitId, null, null, null, null, null, null, null, null,
                MedicalRecordStatus.OPEN, null, null, DOCTOR_ID, NOW.minusSeconds(600), null, null);
    }

    private Visit visit(UUID doctorId) {
        return Visit.restore(UUID.randomUUID(), "VISIT-001", PATIENT_ID, doctorId, null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, NOW.minusSeconds(600), NOW.minusSeconds(500), null,
                "Reason", null, DOCTOR_ID, NOW.minusSeconds(600), null);
    }

    private Patient patient() {
        return Patient.restore(PATIENT_ID, "PAT-001", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                null, null, null, null, null, null, null, null, true, NOW.minusSeconds(600), null, null, DOCTOR_ID);
    }
}

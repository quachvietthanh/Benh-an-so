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
import java.util.ArrayList;
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
import com.benhsoan.domain.prescription.enums.PrescriptionInterconnectionAttemptType;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
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

class RetryPrescriptionInterconnectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T03:00:00Z");
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private final PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
    private final PrescriptionInterconnectionLogRepository logRepository = mock(PrescriptionInterconnectionLogRepository.class);
    private final PrescriptionInterconnectionGatewayPort gatewayPort = mock(PrescriptionInterconnectionGatewayPort.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final ClinicConfigurationRepository clinicConfigurationRepository = mock(ClinicConfigurationRepository.class);
    private final PrescriptionDisplayContextResolver displayContextResolver = mock(PrescriptionDisplayContextResolver.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private RetryPrescriptionInterconnectionService service;

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ADMIN_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(logRepository.findByPrescriptionId(any())).thenReturn(List.of(mock(PrescriptionInterconnectionLog.class)));
        when(prescriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clinicConfigurationRepository.find()).thenReturn(Optional.of(ClinicConfiguration.create(
                "Phong kham A", "1 Duong A", "0900000000", LocalTime.of(8, 0), LocalTime.of(17, 0), NOW)));
        when(displayContextResolver.resolve(any(), any())).thenReturn(
                new PrescriptionDisplayContextResolver.PrescriptionDisplayContext(
                        UUID.randomUUID(), "VISIT-001", PATIENT_ID, "PAT-001", "Nguyen Van A", "Dr. B"));
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient()));
        service = new RetryPrescriptionInterconnectionService(
                prescriptionRepository, logRepository, gatewayPort, patientRepository, clinicConfigurationRepository,
                displayContextResolver, currentUserPort, clockPort, auditLogRepository,
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void adminRetriesFailedPrescriptionAndHistoryRecordsAdminAndRetryAttempt() {
        Prescription prescription = failedPrescription();
        when(prescriptionRepository.findByIdForUpdate(prescription.getId())).thenReturn(Optional.of(prescription));
        when(gatewayPort.submit(any())).thenReturn(
                new PrescriptionInterconnectionGatewayResponse("LT-20260821-000001", "ACCEPTED", NOW));

        var result = service.retry(prescription.getId());

        assertEquals(InterconnectionStatus.SUCCESS, result.status());
        verify(logRepository).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getAttemptNumber() == 2 && log.getAttemptType() == PrescriptionInterconnectionAttemptType.RETRY
                        && log.getAttemptedBy().equals(ADMIN_ID)));
        verify(auditLogRepository).save(any());
    }

    @Test
    void retryRejectsAnyStatusOtherThanFailed() {
        Prescription prescription = pendingPrescription();
        when(prescriptionRepository.findByIdForUpdate(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThrows(ValidationException.class, () -> service.retry(prescription.getId()));
        verify(gatewayPort, never()).submit(any());
    }

    @Test
    void nonAdminCannotRetry() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.retry(UUID.randomUUID()));
        verify(prescriptionRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void timeoutThenAdminRetryCreatesTwoLogsWithCorrectActorsTimesAndOutcomes() {
        Prescription prescription = pendingPrescription();
        UUID visitId = UUID.randomUUID();
        List<PrescriptionInterconnectionLog> history = new ArrayList<>();
        when(logRepository.findByPrescriptionId(prescription.getId())).thenAnswer(invocation -> List.copyOf(history));
        when(logRepository.save(any())).thenAnswer(invocation -> {
            PrescriptionInterconnectionLog log = invocation.getArgument(0);
            history.add(log);
            return log;
        });
        when(prescriptionRepository.findByIdForUpdate(prescription.getId())).thenReturn(Optional.of(prescription));
        when(medicalRecordRepository.findById(prescription.getMedicalRecordId()))
                .thenReturn(Optional.of(medicalRecord(visitId)));
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit(DOCTOR_ID)));
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(DOCTOR_ID, ADMIN_ID);
        when(clockPort.now()).thenReturn(
                NOW, NOW.plusSeconds(1), NOW.plusSeconds(2), NOW.plusSeconds(3));
        when(gatewayPort.submit(any()))
                .thenThrow(new RuntimeException("Gateway timeout"))
                .thenReturn(new PrescriptionInterconnectionGatewayResponse("LT-20260821-000001", "ACCEPTED", NOW));
        SendPrescriptionInterconnectionService sendService = new SendPrescriptionInterconnectionService(
                prescriptionRepository, logRepository, gatewayPort, medicalRecordRepository, visitRepository,
                patientRepository, clinicConfigurationRepository, displayContextResolver, currentUserPort,
                clockPort, auditLogRepository, new ObjectMapper().findAndRegisterModules());

        var failed = sendService.send(prescription.getId());
        var retried = service.retry(prescription.getId());

        assertEquals(InterconnectionStatus.FAILED, failed.status());
        assertEquals(InterconnectionStatus.SUCCESS, retried.status());
        assertEquals(2, history.size());
        assertEquals(PrescriptionInterconnectionAttemptType.SEND, history.get(0).getAttemptType());
        assertEquals("FAILED", history.get(0).getOutcome().name());
        assertEquals(DOCTOR_ID, history.get(0).getAttemptedBy());
        assertEquals(NOW, history.get(0).getStartedAt());
        assertEquals(NOW.plusSeconds(1), history.get(0).getCompletedAt());
        assertEquals(PrescriptionInterconnectionAttemptType.RETRY, history.get(1).getAttemptType());
        assertEquals("SUCCESS", history.get(1).getOutcome().name());
        assertEquals(ADMIN_ID, history.get(1).getAttemptedBy());
        assertEquals(NOW.plusSeconds(2), history.get(1).getStartedAt());
        assertEquals(NOW.plusSeconds(3), history.get(1).getCompletedAt());
    }

    private Prescription failedPrescription() {
        Prescription prescription = pendingPrescription();
        prescription.markInterconnectionFailed("Gateway unavailable", NOW.minusSeconds(60));
        return prescription;
    }

    private Prescription pendingPrescription() {
        UUID prescriptionId = UUID.randomUUID();
        PrescriptionItem item = PrescriptionItem.restore(UUID.randomUUID(), prescriptionId, UUID.randomUUID(),
                "Paracetamol", "Paracetamol", "500 mg", "vien", "1 vien", 2,
                AdministrationRoute.ORAL, 5, 10, null, NOW.minusSeconds(600), null);
        return Prescription.restore(prescriptionId, "RX000001", UUID.randomUUID(),
                PrescriptionStatus.PENDING_DISPENSE, null, UUID.randomUUID(), NOW.minusSeconds(600), null, null,
                List.of(item));
    }

    private Patient patient() {
        return Patient.restore(PATIENT_ID, "PAT-001", "Nguyen Van A", LocalDate.of(1990, 1, 1), Gender.MALE,
                null, null, null, null, null, null, null, null, true, NOW.minusSeconds(600), null, null, ADMIN_ID);
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
}

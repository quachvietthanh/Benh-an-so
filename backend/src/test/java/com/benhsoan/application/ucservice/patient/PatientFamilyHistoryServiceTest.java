package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientFamilyHistory;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.patient.AddPatientFamilyHistoryCommand;
import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientFamilyHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class PatientFamilyHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID CATALOG_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final PatientFamilyHistoryRepository familyHistoryRepository = mock(PatientFamilyHistoryRepository.class);
    private final DiagnosisCatalogRepository diagnosisCatalogRepository = mock(DiagnosisCatalogRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private AddPatientFamilyHistoryService addService;
    private GetPatientFamilyHistoryService getService;

    @BeforeEach
    void setUp() {
        PatientFamilyHistoryResultMapper resultMapper = new PatientFamilyHistoryResultMapper();
        addService = new AddPatientFamilyHistoryService(
                patientRepository, familyHistoryRepository, diagnosisCatalogRepository, visitRepository,
                auditLogRepository, currentUserPort, clockPort, resultMapper, new ObjectMapper());
        getService = new GetPatientFamilyHistoryService(patientRepository, familyHistoryRepository, resultMapper);
    }

    private void stubActivePatient() {
        Patient patient = mock(Patient.class);
        when(patient.isActive()).thenReturn(true);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
    }

    @Test
    void createsFamilyHistory() {
        stubActivePatient();
        DiagnosisCatalog catalog = mock(DiagnosisCatalog.class);
        when(diagnosisCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalog));
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(familyHistoryRepository.save(any(PatientFamilyHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatientFamilyHistoryResult result = addService.addFamilyHistory(
                AddPatientFamilyHistoryCommand.builder()
                        .patientId(PATIENT_ID)
                        .relationship("Bố")
                        .diagnosisCatalogId(CATALOG_ID)
                        .build());

        assertEquals("Bố", result.relationship());
        assertEquals(CATALOG_ID, result.diagnosisCatalogId());
        assertTrue(result.active());
        verify(auditLogRepository).save(any());
    }

    @Test
    void rejectsBlankRelationship() {
        assertThrows(ValidationException.class, () -> addService.addFamilyHistory(
                AddPatientFamilyHistoryCommand.builder()
                        .patientId(PATIENT_ID)
                        .relationship("  ")
                        .diagnosisCatalogId(CATALOG_ID)
                        .build()));
    }

    @Test
    void rejectsMissingDiagnosisCatalogId() {
        assertThrows(ValidationException.class, () -> addService.addFamilyHistory(
                AddPatientFamilyHistoryCommand.builder()
                        .patientId(PATIENT_ID)
                        .relationship("Bố")
                        .diagnosisCatalogId(null)
                        .build()));
    }

    @Test
    void rejectsUnknownDiseaseCatalog() {
        stubActivePatient();
        when(diagnosisCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.empty());

        assertThrows(DiagnosisCatalogNotFoundException.class, () -> addService.addFamilyHistory(
                AddPatientFamilyHistoryCommand.builder()
                        .patientId(PATIENT_ID)
                        .relationship("Bố")
                        .diagnosisCatalogId(CATALOG_ID)
                        .build()));
    }

    @Test
    void retrievesFamilyHistoryForPatient() {
        Patient patient = mock(Patient.class);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        PatientFamilyHistory familyHistory = PatientFamilyHistory.create(
                PATIENT_ID, "Bố", CATALOG_ID, null, ACTOR_ID, NOW);
        when(familyHistoryRepository.findByPatientIdAndActiveTrue(PATIENT_ID)).thenReturn(List.of(familyHistory));

        List<PatientFamilyHistoryResult> results = getService.getFamilyHistory(PATIENT_ID);

        assertEquals(1, results.size());
        assertEquals("Bố", results.get(0).relationship());
    }

    @Test
    void retrieveRejectsUnknownPatient() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());
        assertThrows(PatientNotFoundException.class, () -> getService.getFamilyHistory(PATIENT_ID));
    }
}

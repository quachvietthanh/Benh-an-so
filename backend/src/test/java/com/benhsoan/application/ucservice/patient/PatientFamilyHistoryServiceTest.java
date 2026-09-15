package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientFamilyHistory;
import com.benhsoan.domain.patient.exception.PatientInactiveException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
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
    private static final UUID VISIT_ID = UUID.randomUUID();

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
        getService = new GetPatientFamilyHistoryService(patientRepository, familyHistoryRepository,
                diagnosisCatalogRepository, resultMapper);
    }

    private void stubActivePatient() {
        Patient patient = mock(Patient.class);
        when(patient.isActive()).thenReturn(true);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
    }

    private DiagnosisCatalog activeCatalog() {
        DiagnosisCatalog catalog = mock(DiagnosisCatalog.class);
        when(catalog.isActive()).thenReturn(true);
        when(catalog.getCode()).thenReturn("E11.9");
        when(catalog.getName()).thenReturn("Đái tháo đường type 2");
        return catalog;
    }

    private void stubCatalog() {
        DiagnosisCatalog catalog = activeCatalog();
        when(diagnosisCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalog));
    }

    @Test
    void createsFamilyHistory() {
        stubActivePatient();
        DiagnosisCatalog catalog = activeCatalog();
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
        assertEquals("E11.9", result.diagnosisCode());
        assertEquals("Đái tháo đường type 2", result.diagnosisName());
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
    void rejectsInactiveDiseaseCatalog() {
        stubActivePatient();
        DiagnosisCatalog catalog = mock(DiagnosisCatalog.class);
        when(catalog.isActive()).thenReturn(false);
        when(diagnosisCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalog));

        assertThrows(ValidationException.class, () -> addService.addFamilyHistory(
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

        DiagnosisCatalog catalog = mock(DiagnosisCatalog.class);
        when(catalog.getId()).thenReturn(CATALOG_ID);
        when(catalog.getCode()).thenReturn("E11.9");
        when(catalog.getName()).thenReturn("Đái tháo đường type 2");
        when(diagnosisCatalogRepository.findAllByIds(any())).thenReturn(List.of(catalog));

        List<PatientFamilyHistoryResult> results = getService.getFamilyHistory(PATIENT_ID);

        assertEquals(1, results.size());
        assertEquals("Bố", results.get(0).relationship());
        assertEquals("E11.9", results.get(0).diagnosisCode());
        assertEquals("Đái tháo đường type 2", results.get(0).diagnosisName());
    }

    @Test
    void recordsVisitIdInAuditDetailWhenProvided() {
        stubActivePatient();
        DiagnosisCatalog catalog = activeCatalog();
        when(diagnosisCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalog));
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(familyHistoryRepository.save(any(PatientFamilyHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Visit visit = mock(Visit.class);
        when(visit.getPatientId()).thenReturn(PATIENT_ID);
        when(visit.isActive()).thenReturn(true);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        addService.addFamilyHistory(AddPatientFamilyHistoryCommand.builder()
                .patientId(PATIENT_ID)
                .relationship("Bố")
                .diagnosisCatalogId(CATALOG_ID)
                .visitId(VISIT_ID)
                .build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertTrue(captor.getValue().getDetail().contains(VISIT_ID.toString()));
    }

    @Test
    void retrieveRejectsUnknownPatient() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());
        assertThrows(PatientNotFoundException.class, () -> getService.getFamilyHistory(PATIENT_ID));
    }

    @Test
    void rejectsInactivePatient() {
        Patient patient = mock(Patient.class);
        when(patient.isActive()).thenReturn(false);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

        assertThrows(PatientInactiveException.class, () -> addService.addFamilyHistory(
                AddPatientFamilyHistoryCommand.builder()
                        .patientId(PATIENT_ID)
                        .relationship("Bố")
                        .diagnosisCatalogId(CATALOG_ID)
                        .build()));
    }

    @Test
    void rejectsVisitOfAnotherPatient() {
        stubActivePatient();
        stubCatalog();
        Visit visit = mock(Visit.class);
        UUID anotherPatientId = UUID.randomUUID();
        when(visit.getPatientId()).thenReturn(anotherPatientId);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        ValidationException ex = assertThrows(ValidationException.class, () -> addService.addFamilyHistory(
                AddPatientFamilyHistoryCommand.builder()
                        .patientId(PATIENT_ID)
                        .relationship("Bố")
                        .diagnosisCatalogId(CATALOG_ID)
                        .visitId(VISIT_ID)
                        .build()));
        assertEquals("Lượt khám không thuộc về bệnh nhân này.", ex.getMessage());
    }

    @Test
    void rejectsInactiveVisit() {
        stubActivePatient();
        stubCatalog();
        Visit visit = mock(Visit.class);
        when(visit.getPatientId()).thenReturn(PATIENT_ID);
        when(visit.isActive()).thenReturn(false);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        ValidationException ex = assertThrows(ValidationException.class, () -> addService.addFamilyHistory(
                AddPatientFamilyHistoryCommand.builder()
                        .patientId(PATIENT_ID)
                        .relationship("Bố")
                        .diagnosisCatalogId(CATALOG_ID)
                        .visitId(VISIT_ID)
                        .build()));
        assertEquals("Lượt khám đã kết thúc hoặc không còn hiệu lực.", ex.getMessage());
    }

    @Test
    void allowsMultipleFamilyHistoriesWithSameDiagnosisCatalog() {
        stubActivePatient();
        stubCatalog();
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(familyHistoryRepository.save(any(PatientFamilyHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatientFamilyHistoryResult fatherResult = addService.addFamilyHistory(
                AddPatientFamilyHistoryCommand.builder()
                        .patientId(PATIENT_ID)
                        .relationship("Bố")
                        .diagnosisCatalogId(CATALOG_ID)
                        .build());

        PatientFamilyHistoryResult motherResult = addService.addFamilyHistory(
                AddPatientFamilyHistoryCommand.builder()
                        .patientId(PATIENT_ID)
                        .relationship("Mẹ")
                        .diagnosisCatalogId(CATALOG_ID)
                        .build());

        assertEquals("Bố", fatherResult.relationship());
        assertEquals(CATALOG_ID, fatherResult.diagnosisCatalogId());
        assertEquals("Mẹ", motherResult.relationship());
        assertEquals(CATALOG_ID, motherResult.diagnosisCatalogId());
        verify(familyHistoryRepository, times(2)).save(any());
    }
}


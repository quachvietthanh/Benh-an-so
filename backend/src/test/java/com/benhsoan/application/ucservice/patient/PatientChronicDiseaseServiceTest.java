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
import com.benhsoan.domain.patient.PatientChronicDisease;
import com.benhsoan.domain.patient.exception.PatientChronicDiseaseAlreadyExistsException;
import com.benhsoan.domain.patient.exception.PatientInactiveException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.command.patient.AddPatientChronicDiseaseCommand;
import com.benhsoan.port.dto.result.patient.PatientChronicDiseaseResult;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChronicDiseaseRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class PatientChronicDiseaseServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID CATALOG_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID VISIT_ID = UUID.randomUUID();

    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final PatientChronicDiseaseRepository chronicDiseaseRepository = mock(PatientChronicDiseaseRepository.class);
    private final DiagnosisCatalogRepository diagnosisCatalogRepository = mock(DiagnosisCatalogRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private AddPatientChronicDiseaseService addService;
    private GetPatientChronicDiseasesService getService;

    @BeforeEach
    void setUp() {
        PatientChronicDiseaseResultMapper resultMapper = new PatientChronicDiseaseResultMapper();
        addService = new AddPatientChronicDiseaseService(
                patientRepository, chronicDiseaseRepository, diagnosisCatalogRepository, visitRepository,
                auditLogRepository, currentUserPort, clockPort, resultMapper, new ObjectMapper());
        getService = new GetPatientChronicDiseasesService(patientRepository, chronicDiseaseRepository,
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
    void createsChronicDisease() {
        stubActivePatient();
        stubCatalog();
        when(chronicDiseaseRepository.existsByPatientIdAndDiagnosisCatalogIdAndActiveTrue(PATIENT_ID, CATALOG_ID))
                .thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(chronicDiseaseRepository.save(any(PatientChronicDisease.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PatientChronicDiseaseResult result = addService.addChronicDisease(
                AddPatientChronicDiseaseCommand.builder()
                        .patientId(PATIENT_ID)
                        .diagnosisCatalogId(CATALOG_ID)
                        .yearDetected(2015)
                        .notes("Đang điều trị")
                        .build());

        assertEquals(PATIENT_ID, result.patientId());
        assertEquals(CATALOG_ID, result.diagnosisCatalogId());
        assertEquals("E11.9", result.diagnosisCode());
        assertEquals("Đái tháo đường type 2", result.diagnosisName());
        assertEquals(2015, result.yearDetected());
        assertTrue(result.active());
        verify(auditLogRepository).save(any());
    }

    @Test
    void rejectsMissingDiagnosisCatalogId() {
        assertThrows(ValidationException.class, () -> addService.addChronicDisease(
                AddPatientChronicDiseaseCommand.builder()
                        .patientId(PATIENT_ID)
                        .diagnosisCatalogId(null)
                        .build()));
    }

    @Test
    void rejectsUnknownPatient() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());
        assertThrows(PatientNotFoundException.class, () -> addService.addChronicDisease(
                AddPatientChronicDiseaseCommand.builder()
                        .patientId(PATIENT_ID)
                        .diagnosisCatalogId(CATALOG_ID)
                        .build()));
    }

    @Test
    void rejectsUnknownDiseaseCatalog() {
        stubActivePatient();
        when(diagnosisCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.empty());
        assertThrows(DiagnosisCatalogNotFoundException.class, () -> addService.addChronicDisease(
                AddPatientChronicDiseaseCommand.builder()
                        .patientId(PATIENT_ID)
                        .diagnosisCatalogId(CATALOG_ID)
                        .build()));
    }

    @Test
    void rejectsInactiveDiseaseCatalog() {
        stubActivePatient();
        DiagnosisCatalog catalog = mock(DiagnosisCatalog.class);
        when(catalog.isActive()).thenReturn(false);
        when(diagnosisCatalogRepository.findById(CATALOG_ID)).thenReturn(Optional.of(catalog));

        assertThrows(ValidationException.class, () -> addService.addChronicDisease(
                AddPatientChronicDiseaseCommand.builder()
                        .patientId(PATIENT_ID)
                        .diagnosisCatalogId(CATALOG_ID)
                        .build()));
    }

    @Test
    void rejectsDuplicateActiveChronicDisease() {
        stubActivePatient();
        stubCatalog();
        when(chronicDiseaseRepository.existsByPatientIdAndDiagnosisCatalogIdAndActiveTrue(PATIENT_ID, CATALOG_ID))
                .thenReturn(true);

        assertThrows(PatientChronicDiseaseAlreadyExistsException.class, () -> addService.addChronicDisease(
                AddPatientChronicDiseaseCommand.builder()
                        .patientId(PATIENT_ID)
                        .diagnosisCatalogId(CATALOG_ID)
                        .build()));

        verify(chronicDiseaseRepository, never()).save(any());
    }

    @Test
    void retrievesChronicDiseasesForPatient() {
        Patient patient = mock(Patient.class);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        PatientChronicDisease disease = PatientChronicDisease.create(
                PATIENT_ID, CATALOG_ID, 2015, null, ACTOR_ID, NOW);
        when(chronicDiseaseRepository.findByPatientIdAndActiveTrue(PATIENT_ID)).thenReturn(List.of(disease));

        DiagnosisCatalog catalog = mock(DiagnosisCatalog.class);
        when(catalog.getId()).thenReturn(CATALOG_ID);
        when(catalog.getCode()).thenReturn("E11.9");
        when(catalog.getName()).thenReturn("Đái tháo đường type 2");
        when(diagnosisCatalogRepository.findAllByIds(any())).thenReturn(List.of(catalog));

        List<PatientChronicDiseaseResult> results = getService.getChronicDiseases(PATIENT_ID);

        assertEquals(1, results.size());
        assertEquals(CATALOG_ID, results.get(0).diagnosisCatalogId());
        assertEquals("E11.9", results.get(0).diagnosisCode());
        assertEquals("Đái tháo đường type 2", results.get(0).diagnosisName());
    }

    @Test
    void retrievesEmptyChronicDiseasesWithoutCatalogLookup() {
        Patient patient = mock(Patient.class);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(chronicDiseaseRepository.findByPatientIdAndActiveTrue(PATIENT_ID)).thenReturn(List.of());

        List<PatientChronicDiseaseResult> results = getService.getChronicDiseases(PATIENT_ID);

        assertTrue(results.isEmpty());
        verify(diagnosisCatalogRepository, never()).findAllByIds(any());
    }

    @Test
    void retrievesMultipleChronicDiseasesWithSingleBatchLookup() {
        UUID secondCatalogId = UUID.randomUUID();
        Patient patient = mock(Patient.class);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        PatientChronicDisease first = PatientChronicDisease.create(
                PATIENT_ID, CATALOG_ID, 2015, null, ACTOR_ID, NOW);
        PatientChronicDisease second = PatientChronicDisease.create(
                PATIENT_ID, secondCatalogId, 2019, null, ACTOR_ID, NOW);
        when(chronicDiseaseRepository.findByPatientIdAndActiveTrue(PATIENT_ID)).thenReturn(List.of(first, second));

        DiagnosisCatalog catalog = mock(DiagnosisCatalog.class);
        when(catalog.getId()).thenReturn(CATALOG_ID);
        when(catalog.getCode()).thenReturn("E11.9");
        when(catalog.getName()).thenReturn("Đái tháo đường type 2");
        when(diagnosisCatalogRepository.findAllByIds(any())).thenReturn(List.of(catalog));

        List<PatientChronicDiseaseResult> results = getService.getChronicDiseases(PATIENT_ID);

        assertEquals(2, results.size());
        verify(diagnosisCatalogRepository, times(1)).findAllByIds(any());
    }

    @Test
    void recordsVisitIdInAuditDetailWhenProvided() {
        stubActivePatient();
        stubCatalog();
        when(chronicDiseaseRepository.existsByPatientIdAndDiagnosisCatalogIdAndActiveTrue(PATIENT_ID, CATALOG_ID))
                .thenReturn(false);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
        when(chronicDiseaseRepository.save(any(PatientChronicDisease.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Visit visit = mock(Visit.class);
        when(visit.getPatientId()).thenReturn(PATIENT_ID);
        when(visit.isActive()).thenReturn(true);
        when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit));

        addService.addChronicDisease(AddPatientChronicDiseaseCommand.builder()
                .patientId(PATIENT_ID)
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
        assertThrows(PatientNotFoundException.class, () -> getService.getChronicDiseases(PATIENT_ID));
    }

    @Test
    void rejectsInactivePatient() {
        Patient patient = mock(Patient.class);
        when(patient.isActive()).thenReturn(false);
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

        assertThrows(PatientInactiveException.class, () -> addService.addChronicDisease(
                AddPatientChronicDiseaseCommand.builder()
                        .patientId(PATIENT_ID)
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

        ValidationException ex = assertThrows(ValidationException.class, () -> addService.addChronicDisease(
                AddPatientChronicDiseaseCommand.builder()
                        .patientId(PATIENT_ID)
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

        ValidationException ex = assertThrows(ValidationException.class, () -> addService.addChronicDisease(
                AddPatientChronicDiseaseCommand.builder()
                        .patientId(PATIENT_ID)
                        .diagnosisCatalogId(CATALOG_ID)
                        .visitId(VISIT_ID)
                        .build()));
        assertEquals("Lượt khám đã kết thúc hoặc không còn hiệu lực.", ex.getMessage());
    }
}

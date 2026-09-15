package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChronicDisease;
import com.benhsoan.domain.patient.exception.PatientChronicDiseaseNotFoundException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.patient.DeletePatientChronicDiseaseCommand;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChronicDiseaseRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class DeletePatientChronicDiseaseServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID OTHER_PATIENT_ID = UUID.randomUUID();
    private static final UUID CHRONIC_DISEASE_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final PatientChronicDiseaseRepository chronicDiseaseRepository = mock(PatientChronicDiseaseRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DeletePatientChronicDiseaseService service;

    @BeforeEach
    void setUp() {
        service = new DeletePatientChronicDiseaseService(
                patientRepository, chronicDiseaseRepository, auditLogRepository,
                currentUserPort, clockPort, objectMapper);
    }

    private Patient activePatient() {
        Patient patient = mock(Patient.class);
        when(patient.isActive()).thenReturn(true);
        return patient;
    }

    private PatientChronicDisease activeChronicDisease(UUID patientId) {
        return PatientChronicDisease.create(
                patientId, UUID.randomUUID(), 2015, null, ACTOR_ID, NOW);
    }

    private DeletePatientChronicDiseaseCommand command(String reason) {
        return DeletePatientChronicDiseaseCommand.builder()
                .patientId(PATIENT_ID)
                .chronicDiseaseId(CHRONIC_DISEASE_ID)
                .reason(reason)
                .build();
    }

    private void stubActivePatientAndDisease(PatientChronicDisease disease) {
        Patient patient = activePatient();
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(chronicDiseaseRepository.findById(CHRONIC_DISEASE_ID)).thenReturn(Optional.of(disease));
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void softDeletesChronicDiseaseWithReason() {
        PatientChronicDisease disease = activeChronicDisease(PATIENT_ID);
        stubActivePatientAndDisease(disease);

        service.deleteChronicDisease(command("Đã khỏi bệnh"));

        assertFalse(disease.isActive());
        verify(chronicDiseaseRepository).save(disease);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(ActionType.DELETE, audit.getActionType());
        assertEquals(ResourceType.PATIENT_CHRONIC_DISEASE, audit.getResourceType());
        assertEquals(disease.getId(), audit.getResourceId());
        assertEquals("Đã khỏi bệnh", reasonFromDetail(audit.getDetail()));
    }

    @Test
    void recordsNullDetailWhenReasonAbsent() {
        PatientChronicDisease disease = activeChronicDisease(PATIENT_ID);
        stubActivePatientAndDisease(disease);

        service.deleteChronicDisease(command(null));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getDetail());
    }

    @Test
    void rejectsUnknownPatient() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.deleteChronicDisease(command(null)));

        verify(chronicDiseaseRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownChronicDisease() {
        Patient patient = activePatient();
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(chronicDiseaseRepository.findById(CHRONIC_DISEASE_ID)).thenReturn(Optional.empty());

        assertThrows(PatientChronicDiseaseNotFoundException.class,
                () -> service.deleteChronicDisease(command(null)));
    }

    @Test
    void rejectsChronicDiseaseOfAnotherPatient() {
        PatientChronicDisease other = activeChronicDisease(OTHER_PATIENT_ID);
        Patient patient = activePatient();
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(chronicDiseaseRepository.findById(CHRONIC_DISEASE_ID)).thenReturn(Optional.of(other));

        assertThrows(PatientChronicDiseaseNotFoundException.class,
                () -> service.deleteChronicDisease(command(null)));
    }

    @Test
    void rejectsAlreadyInactiveChronicDisease() {
        PatientChronicDisease disease = activeChronicDisease(PATIENT_ID);
        disease.deactivate(ACTOR_ID, NOW);
        Patient patient = activePatient();
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(chronicDiseaseRepository.findById(CHRONIC_DISEASE_ID)).thenReturn(Optional.of(disease));

        assertThrows(PatientChronicDiseaseNotFoundException.class,
                () -> service.deleteChronicDisease(command(null)));
    }

    private String reasonFromDetail(String detail) {
        try {
            assertTrue(detail != null && !detail.isBlank(), "Audit detail must not be empty for a provided reason.");
            var node = objectMapper.readTree(detail);
            assertTrue(node.has("reason"), "Audit detail must contain a 'reason' field: " + detail);
            return node.get("reason").asText();
        } catch (Exception exception) {
            throw new AssertionError("Audit detail is not valid JSON: " + detail, exception);
        }
    }
}

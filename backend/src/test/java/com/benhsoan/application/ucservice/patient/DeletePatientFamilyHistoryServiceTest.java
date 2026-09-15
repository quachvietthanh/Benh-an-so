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
import com.benhsoan.domain.patient.PatientFamilyHistory;
import com.benhsoan.domain.patient.exception.PatientFamilyHistoryNotFoundException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.patient.DeletePatientFamilyHistoryCommand;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientFamilyHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.databind.ObjectMapper;

class DeletePatientFamilyHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID OTHER_PATIENT_ID = UUID.randomUUID();
    private static final UUID FAMILY_HISTORY_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final PatientFamilyHistoryRepository familyHistoryRepository = mock(PatientFamilyHistoryRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DeletePatientFamilyHistoryService service;

    @BeforeEach
    void setUp() {
        service = new DeletePatientFamilyHistoryService(
                patientRepository, familyHistoryRepository, auditLogRepository,
                currentUserPort, clockPort, objectMapper);
    }

    private Patient activePatient() {
        Patient patient = mock(Patient.class);
        when(patient.isActive()).thenReturn(true);
        return patient;
    }

    private PatientFamilyHistory activeFamilyHistory(UUID patientId) {
        return PatientFamilyHistory.create(
                patientId, "Bố", UUID.randomUUID(), null, ACTOR_ID, NOW);
    }

    private DeletePatientFamilyHistoryCommand command(String reason) {
        return DeletePatientFamilyHistoryCommand.builder()
                .patientId(PATIENT_ID)
                .familyHistoryId(FAMILY_HISTORY_ID)
                .reason(reason)
                .build();
    }

    private void stubActivePatientAndHistory(PatientFamilyHistory history) {
        Patient patient = activePatient();
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(familyHistoryRepository.findById(FAMILY_HISTORY_ID)).thenReturn(Optional.of(history));
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void softDeletesFamilyHistoryWithReason() {
        PatientFamilyHistory history = activeFamilyHistory(PATIENT_ID);
        stubActivePatientAndHistory(history);

        service.deleteFamilyHistory(command("Đã khỏi bệnh"));

        assertFalse(history.isActive());
        verify(familyHistoryRepository).save(history);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(ActionType.DELETE, audit.getActionType());
        assertEquals(ResourceType.PATIENT_FAMILY_HISTORY, audit.getResourceType());
        assertEquals(history.getId(), audit.getResourceId());
        assertEquals("Đã khỏi bệnh", reasonFromDetail(audit.getDetail()));
    }

    @Test
    void recordsNullDetailWhenReasonAbsent() {
        PatientFamilyHistory history = activeFamilyHistory(PATIENT_ID);
        stubActivePatientAndHistory(history);

        service.deleteFamilyHistory(command(null));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getDetail());
    }

    @Test
    void rejectsUnknownPatient() {
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.deleteFamilyHistory(command(null)));

        verify(familyHistoryRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownFamilyHistory() {
        Patient patient = activePatient();
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(familyHistoryRepository.findById(FAMILY_HISTORY_ID)).thenReturn(Optional.empty());

        assertThrows(PatientFamilyHistoryNotFoundException.class,
                () -> service.deleteFamilyHistory(command(null)));
    }

    @Test
    void rejectsFamilyHistoryOfAnotherPatient() {
        PatientFamilyHistory other = activeFamilyHistory(OTHER_PATIENT_ID);
        Patient patient = activePatient();
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(familyHistoryRepository.findById(FAMILY_HISTORY_ID)).thenReturn(Optional.of(other));

        assertThrows(PatientFamilyHistoryNotFoundException.class,
                () -> service.deleteFamilyHistory(command(null)));
    }

    @Test
    void rejectsAlreadyInactiveFamilyHistory() {
        PatientFamilyHistory history = activeFamilyHistory(PATIENT_ID);
        history.deactivate(ACTOR_ID, NOW);
        Patient patient = activePatient();
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(familyHistoryRepository.findById(FAMILY_HISTORY_ID)).thenReturn(Optional.of(history));

        assertThrows(PatientFamilyHistoryNotFoundException.class,
                () -> service.deleteFamilyHistory(command(null)));
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

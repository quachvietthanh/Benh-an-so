package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.enums.PatientChangeAction;
import com.benhsoan.domain.patient.enums.PregnancyStatus;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class UpdatePatientPregnancyStatusServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final PatientResultMapper patientResultMapper = mock(PatientResultMapper.class);
    private final PatientChangeLogRepository patientChangeLogRepository = mock(PatientChangeLogRepository.class);
    private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private UpdatePatientPregnancyStatusService service;

    @BeforeEach
    void setUp() {
        service = new UpdatePatientPregnancyStatusService(
                patientRepository,
                patientResultMapper,
                patientChangeLogRepository,
                auditLogRepository,
                currentUserPort,
                clockPort);
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR_ID);
    }

    @Test
    void updatesStatusAndWritesChangeLogAndAudit() {
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(PATIENT_ID);
        when(patient.getPregnancyStatus()).thenReturn(PregnancyStatus.NOT_PREGNANT);
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(patientRepository.save(patient)).thenReturn(patient);

        service.update(PATIENT_ID, PregnancyStatus.PREGNANT);

        verify(patient).changePregnancyStatus(PregnancyStatus.PREGNANT);

        ArgumentCaptor<PatientChangeLog> changeLogCaptor = ArgumentCaptor.forClass(PatientChangeLog.class);
        verify(patientChangeLogRepository).save(changeLogCaptor.capture());
        PatientChangeLog changeLog = changeLogCaptor.getValue();
        assertEquals(PATIENT_ID, changeLog.getPatientId());
        assertEquals(ACTOR_ID, changeLog.getChangedBy());
        assertEquals(PatientChangeAction.UPDATE, changeLog.getAction());
        assertEquals(NOW, changeLog.getCreatedAt());

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog auditLog = auditCaptor.getValue();
        assertEquals(ACTOR_ID, auditLog.getUserId());
        assertEquals(ActionType.UPDATE, auditLog.getActionType());
        assertEquals(ResourceType.PATIENT, auditLog.getResourceType());
        assertEquals(PATIENT_ID, auditLog.getResourceId());
        assertEquals(NOW, auditLog.getCreatedAt());
    }

    @Test
    void writesNoLogWhenPatientSaveFails() {
        Patient patient = mock(Patient.class);
        when(patient.getId()).thenReturn(PATIENT_ID);
        when(patientRepository.findByIdForUpdate(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(patientRepository.save(patient)).thenThrow(new RuntimeException("db error"));

        assertThrows(RuntimeException.class, () -> service.update(PATIENT_ID, PregnancyStatus.PREGNANT));

        verify(patientChangeLogRepository, never()).save(any(PatientChangeLog.class));
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void rejectsNullPatientId() {
        assertThrows(com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> service.update(null, PregnancyStatus.PREGNANT));
    }
}

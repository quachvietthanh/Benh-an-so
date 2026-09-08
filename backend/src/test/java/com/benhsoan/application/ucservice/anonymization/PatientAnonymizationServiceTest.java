package com.benhsoan.application.ucservice.anonymization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.anonymization.AnonymizationMode;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.anonymization.UpdateAnonymizationModeCommand;
import com.benhsoan.port.dto.result.anonymization.AnonymizationModeResult;
import com.benhsoan.port.outbound.repository.anonymization.AnonymizationModeRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class PatientAnonymizationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-08T14:30:00Z");
    private static final UUID ACTOR = UUID.randomUUID();

    @Mock
    private AnonymizationModeRepository repository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrentUserPort currentUserPort;
    @Mock
    private ClockPort clockPort;

    private AnonymizationModeState state;
    private PatientAnonymizationService service;

    @BeforeEach
    void setUp() {
        state = new AnonymizationModeState();
        service = new PatientAnonymizationService(repository, state, auditLogRepository, currentUserPort, clockPort);
    }

    @Test
    void defaultsToDisabledWhenNoConfigurationExists() {
        when(repository.find()).thenReturn(Optional.empty());

        AnonymizationModeResult result = service.get();

        assertFalse(result.enabled());
    }

    @Test
    void returnsPersistedState() {
        when(repository.find()).thenReturn(Optional.of(AnonymizationMode.of(true, NOW)));

        AnonymizationModeResult result = service.get();

        assertTrue(result.enabled());
        assertEquals(NOW, result.updatedAt());
    }

    @Test
    void initializeLoadsPersistedStateIntoCache() {
        when(repository.find()).thenReturn(Optional.of(AnonymizationMode.of(true, NOW)));

        service.initialize();

        assertTrue(state.isEnabled());
    }

    @Test
    void updatePersistsAuditsAndUpdatesCache() {
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(repository.save(any(AnonymizationMode.class), eq(ACTOR)))
                .thenReturn(AnonymizationMode.of(true, NOW));

        AnonymizationModeResult result = service.update(new UpdateAnonymizationModeCommand(true));

        assertTrue(result.enabled());
        assertTrue(state.isEnabled());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(ActionType.UPDATE, audit.getActionType());
        assertEquals(ResourceType.CONFIGURATION, audit.getResourceType());
        assertEquals(ACTOR, audit.getUserId());
        assertTrue(audit.getDetail().contains("from false to true"), audit.getDetail());
    }

    @Test
    void updateToFalseDisablesMode() {
        state.setEnabled(true);
        when(clockPort.now()).thenReturn(NOW);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(repository.save(any(AnonymizationMode.class), eq(ACTOR)))
                .thenReturn(AnonymizationMode.of(false, NOW));

        service.update(new UpdateAnonymizationModeCommand(false));

        assertFalse(state.isEnabled());
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertTrue(captor.getValue().getDetail().contains("from true to false"), captor.getValue().getDetail());
    }

    @Test
    void rejectsNullCommand() {
        assertThrows(ValidationException.class, () -> service.update(null));

        verify(auditLogRepository, never()).save(any());
        verify(repository, never()).save(any(AnonymizationMode.class), any());
    }
}

package com.benhsoan.application.ucservice.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.backup.BackupScheduleConfiguration;
import com.benhsoan.port.outbound.repository.backup.BackupScheduleRepositoryPort;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class BackupScheduleServiceTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-24T08:00:00Z");

    private final BackupScheduleRepositoryPort scheduleRepository = mock(BackupScheduleRepositoryPort.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final BackupAuditLogWriter auditLogWriter = mock(BackupAuditLogWriter.class);
    private final BackupAuthorizer authorizer = new BackupAuthorizer(currentUserPort);

    private BackupScheduleService service;

    @BeforeEach
    void setUp() {
        service = new BackupScheduleService(
                scheduleRepository,
                authorizer,
                currentUserPort,
                clockPort,
                auditLogWriter
        );
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ADMIN_ID);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void getScheduleRejectsNonAdmin() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.getSchedule());
    }

    @Test
    void getScheduleReturnsExistingConfig() {
        BackupScheduleConfiguration existing = BackupScheduleConfiguration.createDefault(ADMIN_ID, NOW);
        when(scheduleRepository.find()).thenReturn(Optional.of(existing));

        BackupScheduleConfiguration result = service.getSchedule();

        assertNotNull(result);
        assertEquals("02:00", result.getDailyTime());
    }

    @Test
    void getScheduleCreatesDefaultWhenNotFound() {
        when(scheduleRepository.find()).thenReturn(Optional.empty());
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BackupScheduleConfiguration result = service.getSchedule();

        assertNotNull(result);
        assertEquals("02:00", result.getDailyTime());
        assertFalse(result.isEnabled());
        verify(scheduleRepository).save(any(BackupScheduleConfiguration.class));
    }

    @Test
    void updateScheduleUpdatesAndAudits() {
        BackupScheduleConfiguration existing = BackupScheduleConfiguration.createDefault(ADMIN_ID, NOW);
        when(scheduleRepository.find()).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BackupScheduleConfiguration updated = service.updateSchedule(true, "03:15");

        assertTrue(updated.isEnabled());
        assertEquals("03:15", updated.getDailyTime());
        assertEquals("0 15 3 * * *", updated.getCronExpression());

        verify(scheduleRepository).save(existing);
        verify(auditLogWriter).write(eq(ADMIN_ID), eq(ActionType.UPDATE), eq(existing.getId()), any());
    }

    @Test
    void dismissAlertDeactivatesAlert() {
        BackupScheduleConfiguration existing = BackupScheduleConfiguration.createDefault(ADMIN_ID, NOW);
        existing.recordFailure(NOW, "Disk space full");
        assertTrue(existing.isAlertActive());

        when(scheduleRepository.find()).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BackupScheduleConfiguration dismissed = service.dismissAlert();

        assertFalse(dismissed.isAlertActive());
        verify(scheduleRepository).save(existing);
        verify(auditLogWriter).write(eq(ADMIN_ID), eq(ActionType.UPDATE), eq(existing.getId()), any());
    }
}

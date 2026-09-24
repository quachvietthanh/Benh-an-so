package com.benhsoan.application.ucservice.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.backup.BackupSchedule;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.backup.UpdateBackupScheduleCommand;
import com.benhsoan.port.dto.result.BackupScheduleResult;
import com.benhsoan.port.outbound.repository.backup.BackupScheduleRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class BackupScheduleConfigurationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T03:00:00Z");

    private final BackupScheduleRepository scheduleRepository = mock(BackupScheduleRepository.class);
    private final AdminOperationAuditService adminOperationAuditService = mock(AdminOperationAuditService.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final BackupAuthorizer authorizer = new BackupAuthorizer(currentUserPort);

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void getReturnsScheduleForAdmin() {
        GetBackupScheduleService service = new GetBackupScheduleService(scheduleRepository, authorizer, clockPort);
        when(scheduleRepository.find()).thenReturn(
                Optional.of(BackupSchedule.create(true, LocalTime.of(2, 0), NOW)));

        BackupScheduleResult result = service.get();

        assertTrue(result.enabled());
        assertEquals(LocalTime.of(2, 0), result.backupTime());
    }

    @Test
    void getDefaultsToDisabledWhenMissing() {
        GetBackupScheduleService service = new GetBackupScheduleService(scheduleRepository, authorizer, clockPort);
        when(scheduleRepository.find()).thenReturn(Optional.empty());

        BackupScheduleResult result = service.get();

        assertFalse(result.enabled());
        assertEquals(LocalTime.of(2, 0), result.backupTime());
    }

    @Test
    void getRejectsNonAdmin() {
        GetBackupScheduleService service = new GetBackupScheduleService(scheduleRepository, authorizer, clockPort);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class, service::get);
    }

    @Test
    void updatePersistsAndAudits() {
        UpdateBackupScheduleService service = new UpdateBackupScheduleService(
                scheduleRepository, authorizer, adminOperationAuditService, currentUserPort, clockPort);
        BackupSchedule existing = BackupSchedule.create(false, LocalTime.of(2, 0), NOW);
        when(scheduleRepository.find()).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BackupScheduleResult result = service.update(
                new UpdateBackupScheduleCommand(true, LocalTime.of(3, 30)));

        assertTrue(result.enabled());
        assertEquals(LocalTime.of(3, 30), result.backupTime());
        verify(adminOperationAuditService).record(
                any(), eq(ActionType.UPDATE), eq(ResourceType.CONFIGURATION), isNull(), any(), any(), eq(NOW));
    }

    @Test
    void updateRejectsNonAdmin() {
        UpdateBackupScheduleService service = new UpdateBackupScheduleService(
                scheduleRepository, authorizer, adminOperationAuditService, currentUserPort, clockPort);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.update(new UpdateBackupScheduleCommand(true, LocalTime.NOON)));
    }

    @Test
    void updateRejectsNullBackupTime() {
        UpdateBackupScheduleService service = new UpdateBackupScheduleService(
                scheduleRepository, authorizer, adminOperationAuditService, currentUserPort, clockPort);

        assertThrows(ValidationException.class,
                () -> service.update(new UpdateBackupScheduleCommand(true, null)));
    }
}

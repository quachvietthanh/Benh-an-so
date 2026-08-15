package com.benhsoan.application.ucservice.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.domain.backup.exception.BackupExecutionException;
import com.benhsoan.port.dto.command.backup.CreateBackupCommand;
import com.benhsoan.port.dto.result.BackupResult;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class CreateBackupServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-14T08:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();

    private final BackupRecordLifecycleService lifecycleService = mock(BackupRecordLifecycleService.class);
    private final BackupSnapshotExportService snapshotExportService = mock(BackupSnapshotExportService.class);
    private final BackupCodeGenerator backupCodeGenerator = mock(BackupCodeGenerator.class);
    private final BackupAuditLogWriter auditLogWriter = mock(BackupAuditLogWriter.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private final BackupAuthorizer authorizer = new BackupAuthorizer(currentUserPort);
    private final BackupResultMapper resultMapper = new BackupResultMapper();

    private CreateBackupService service;
    private BackupRecord createdRecord;

    @BeforeEach
    void setUp() {
        service = new CreateBackupService(
                lifecycleService,
                snapshotExportService,
                backupCodeGenerator,
                auditLogWriter,
                resultMapper,
                authorizer,
                currentUserPort,
                clockPort
        );

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(clockPort.now()).thenReturn(NOW);
        when(backupCodeGenerator.generate()).thenReturn("BKP-20260814-0001");
        when(lifecycleService.createInProgress(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    createdRecord = BackupRecord.create(
                            invocation.getArgument(0),
                            invocation.getArgument(1),
                            invocation.getArgument(2),
                            invocation.getArgument(3),
                            invocation.getArgument(4)
                    );
                    return createdRecord;
                });
        when(lifecycleService.markSuccess(any(UUID.class), any(BackupSnapshot.class)))
                .thenAnswer(invocation -> {
                    BackupSnapshot snapshot = invocation.getArgument(1);
                    createdRecord.markSuccess(snapshot.fileName(), snapshot.content().length);
                    return createdRecord;
                });
    }

    @Test
    void createsSuccessfulBackupAndWritesAuditLog() {
        when(snapshotExportService.export("BKP-20260814-0001"))
                .thenReturn(new BackupSnapshot("BKP-20260814-0001.json", new byte[]{1, 2, 3}));

        BackupResult result = service.create(new CreateBackupCommand(BackupType.MANUAL, "Daily backup"));

        assertEquals("BKP-20260814-0001", result.backupCode());
        assertEquals("BKP-20260814-0001.json", result.fileName());
        assertEquals(3L, result.fileSize());
        assertEquals(BackupStatus.SUCCESS, result.status());
        assertEquals(BackupType.MANUAL, result.backupType());
        assertEquals(ACTOR, result.createdBy());

        verify(auditLogWriter).write(eq(ACTOR), eq(ActionType.BACKUP), any(UUID.class), any(String.class));
    }

    @Test
    void defaultsBackupTypeToFullWhenMissing() {
        when(snapshotExportService.export("BKP-20260814-0001"))
                .thenReturn(new BackupSnapshot("BKP-20260814-0001.json", new byte[]{1}));

        BackupResult result = service.create(new CreateBackupCommand(null, null));

        assertEquals(BackupType.FULL, result.backupType());
    }

    @Test
    void marksFailedAndThrowsWhenExportFails() {
        when(snapshotExportService.export("BKP-20260814-0001"))
                .thenThrow(new RuntimeException("disk full"));

        assertThrows(BackupExecutionException.class,
                () -> service.create(new CreateBackupCommand(BackupType.FULL, null)));

        verify(lifecycleService).markFailed(any(UUID.class));
        verify(auditLogWriter, never()).write(any(), any(), any(), any());
    }

    @Test
    void rejectsNonAdmin() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.create(new CreateBackupCommand(BackupType.FULL, null)));

        verify(snapshotExportService, never()).export(any());
    }
}

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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.domain.backup.exception.BackupNotFoundException;
import com.benhsoan.domain.backup.exception.InvalidBackupStatusException;
import com.benhsoan.port.dto.result.BackupResult;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class RestoreBackupServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-14T08:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID BACKUP_ID = UUID.randomUUID();

    private final BackupRecordRepository backupRecordRepository = mock(BackupRecordRepository.class);
    private final DatabaseBackupStoragePort storagePort = mock(DatabaseBackupStoragePort.class);
    private final BackupAuditLogWriter auditLogWriter = mock(BackupAuditLogWriter.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private final BackupAuthorizer authorizer = new BackupAuthorizer(currentUserPort);
    private final BackupResultMapper resultMapper = new BackupResultMapper();

    private RestoreBackupService service;

    @BeforeEach
    void setUp() {
        service = new RestoreBackupService(
                backupRecordRepository,
                storagePort,
                auditLogWriter,
                resultMapper,
                authorizer,
                currentUserPort,
                clockPort
        );

        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void restoresSuccessfulBackupAndWritesAuditLog() {
        BackupRecord record = recordWithStatus(BackupStatus.SUCCESS);
        when(backupRecordRepository.findById(BACKUP_ID)).thenReturn(Optional.of(record));
        when(backupRecordRepository.save(any(BackupRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BackupResult result = service.restore(BACKUP_ID);

        verify(storagePort).restoreSnapshot("BKP-20260814-0001.json");
        assertEquals(ACTOR, result.restoredBy());
        assertEquals(NOW, result.restoredAt());
        verify(auditLogWriter).write(eq(ACTOR), eq(ActionType.RESTORE), eq(BACKUP_ID), any(String.class));
    }

    @Test
    void rejectsRestoreWhenStatusIsFailed() {
        when(backupRecordRepository.findById(BACKUP_ID))
                .thenReturn(Optional.of(recordWithStatus(BackupStatus.FAILED)));

        assertThrows(InvalidBackupStatusException.class, () -> service.restore(BACKUP_ID));

        verify(storagePort, never()).restoreSnapshot(any());
    }

    @Test
    void rejectsRestoreWhenStatusIsInProgress() {
        when(backupRecordRepository.findById(BACKUP_ID))
                .thenReturn(Optional.of(recordWithStatus(BackupStatus.IN_PROGRESS)));

        assertThrows(InvalidBackupStatusException.class, () -> service.restore(BACKUP_ID));

        verify(storagePort, never()).restoreSnapshot(any());
    }

    @Test
    void rejectsRestoreWhenBackupNotFound() {
        when(backupRecordRepository.findById(BACKUP_ID)).thenReturn(Optional.empty());

        assertThrows(BackupNotFoundException.class, () -> service.restore(BACKUP_ID));
    }

    @Test
    void rejectsNonAdmin() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.restore(BACKUP_ID));

        verify(backupRecordRepository, never()).findById(any());
    }

    private BackupRecord recordWithStatus(BackupStatus status) {
        return BackupRecord.restore(
                BACKUP_ID,
                "BKP-20260814-0001",
                "BKP-20260814-0001.json",
                100L,
                status,
                BackupType.FULL,
                "desc",
                ACTOR,
                NOW,
                null,
                null
        );
    }
}

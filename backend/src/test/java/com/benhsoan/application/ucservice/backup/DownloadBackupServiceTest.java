package com.benhsoan.application.ucservice.backup;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.domain.backup.exception.BackupNotFoundException;
import com.benhsoan.port.dto.result.BackupDownloadResult;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class DownloadBackupServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-14T08:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();

    private final BackupRecordRepository backupRecordRepository = mock(BackupRecordRepository.class);
    private final DatabaseBackupStoragePort storagePort = mock(DatabaseBackupStoragePort.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);
    private final AdminOperationAuditService adminOperationAuditService = mock(AdminOperationAuditService.class);

    private final BackupAuthorizer authorizer = new BackupAuthorizer(currentUserPort);

    private DownloadBackupService service;

    @BeforeEach
    void setUp() {
        service = new DownloadBackupService(
                backupRecordRepository,
                storagePort,
                authorizer,
                adminOperationAuditService,
                currentUserPort,
                clockPort
        );

        when(currentUserPort.getCurrentUserId()).thenReturn(ACTOR);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void downloadSuccessfully_recordsAuditLog() {
        UUID backupId = UUID.randomUUID();
        BackupRecord record = BackupRecord.restore(
                backupId,
                "BK-20260814-001",
                "backup_001.json",
                1024L,
                BackupStatus.SUCCESS,
                BackupType.MANUAL,
                "Test backup",
                null,
                ACTOR,
                NOW,
                null,
                null
        );
        byte[] content = "{\"data\":\"test\"}".getBytes();
        BackupSnapshot snapshot = new BackupSnapshot("backup_001.json", content);

        when(backupRecordRepository.findById(backupId)).thenReturn(Optional.of(record));
        when(storagePort.loadSnapshot("backup_001.json")).thenReturn(snapshot);

        BackupDownloadResult result = service.download(backupId);

        assertEquals(backupId, result.id());
        assertEquals("backup_001.json", result.fileName());
        assertEquals("application/json", result.contentType());
        assertArrayEquals(content, result.content());

        verify(adminOperationAuditService).record(
                eq(ACTOR),
                eq(ActionType.EXPORT),
                eq(ResourceType.SYSTEM_BACKUP),
                eq(backupId),
                eq(null),
                any(),
                eq(NOW)
        );
    }

    @Test
    void download_throwsBackupNotFoundException_whenNotFound() {
        UUID backupId = UUID.randomUUID();
        when(backupRecordRepository.findById(backupId)).thenReturn(Optional.empty());

        assertThrows(BackupNotFoundException.class, () -> service.download(backupId));
    }

    @Test
    void download_requiresAdminRole() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        UUID backupId = UUID.randomUUID();

        assertThrows(AccessDeniedException.class, () -> service.download(backupId));
    }
}

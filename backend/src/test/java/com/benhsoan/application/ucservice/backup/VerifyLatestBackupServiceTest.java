package com.benhsoan.application.ucservice.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.benhsoan.port.dto.result.BackupIntegrityResult;
import com.benhsoan.port.outbound.backup.BackupVerification;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class VerifyLatestBackupServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T03:00:00Z");

    private final BackupRecordRepository backupRecordRepository = mock(BackupRecordRepository.class);
    private final DatabaseBackupStoragePort storagePort = mock(DatabaseBackupStoragePort.class);
    private final BackupAuditLogWriter auditLogWriter = mock(BackupAuditLogWriter.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private final BackupAuthorizer authorizer = new BackupAuthorizer(currentUserPort);
    private VerifyLatestBackupService service;

    @BeforeEach
    void setUp() {
        service = new VerifyLatestBackupService(
                backupRecordRepository, storagePort, authorizer, auditLogWriter, currentUserPort, clockPort);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(clockPort.now()).thenReturn(NOW);
    }

    private BackupRecord successRecord() {
        BackupRecord record = BackupRecord.create(
                "BKP-20260924-0001", BackupType.SCHEDULED, null, UUID.randomUUID(), NOW);
        record.markSuccess("BKP-20260924-0001.json", 1024L);
        return record;
    }

    @Test
    void reportsValidForReadableAndCompleteSnapshot() {
        when(backupRecordRepository.findTopByStatusOrderByCreatedAtDesc(BackupStatus.SUCCESS))
                .thenReturn(Optional.of(successRecord()));
        when(storagePort.verifySnapshot("BKP-20260924-0001.json"))
                .thenReturn(BackupVerification.valid(5, 10L));

        BackupIntegrityResult result = service.verifyLatest();

        assertTrue(result.valid());
        assertEquals(5, result.tableCount());
        assertEquals(10L, result.rowCount());
        assertEquals("BKP-20260924-0001", result.backupCode());
        verify(auditLogWriter).write(any(), eq(ActionType.BACKUP_VERIFY), any(), anyString());
    }

    @Test
    void reportsInvalidForUnreadableSnapshot() {
        when(backupRecordRepository.findTopByStatusOrderByCreatedAtDesc(BackupStatus.SUCCESS))
                .thenReturn(Optional.of(successRecord()));
        when(storagePort.verifySnapshot("BKP-20260924-0001.json"))
                .thenReturn(BackupVerification.invalid("Backup file not found"));

        BackupIntegrityResult result = service.verifyLatest();

        assertFalse(result.valid());
        assertEquals("Backup file not found", result.reason());
    }

    @Test
    void reportsInvalidWhenNoSuccessfulBackupExists() {
        when(backupRecordRepository.findTopByStatusOrderByCreatedAtDesc(BackupStatus.SUCCESS))
                .thenReturn(Optional.empty());

        BackupIntegrityResult result = service.verifyLatest();

        assertFalse(result.valid());
        assertEquals("No successful backup found.", result.reason());
        verifyNoInteractions(auditLogWriter);
    }

    @Test
    void rejectsNonAdmin() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class, service::verifyLatest);
        verifyNoInteractions(storagePort);
    }
}

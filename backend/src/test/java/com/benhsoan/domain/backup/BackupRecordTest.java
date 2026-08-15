package com.benhsoan.domain.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.domain.backup.exception.InvalidBackupStatusException;
import com.benhsoan.domain.shared.exception.ValidationException;

class BackupRecordTest {

    private static final Instant NOW = Instant.parse("2026-08-14T08:00:00Z");
    private static final UUID ACTOR = UUID.randomUUID();

    @Test
    void createStartsInProgressWithoutRestoreMetadata() {
        BackupRecord record = BackupRecord.create("BKP-20260814-0001", BackupType.FULL, "Manual backup", ACTOR, NOW);

        assertEquals(BackupStatus.IN_PROGRESS, record.getStatus());
        assertNull(record.getFileName());
        assertNull(record.getRestoredAt());
        assertNull(record.getRestoredBy());
        assertFalse(record.isRestorable());
    }

    @Test
    void markSuccessTransitionsInProgressToSuccess() {
        BackupRecord record = BackupRecord.create("BKP-20260814-0001", BackupType.FULL, null, ACTOR, NOW);

        record.markSuccess("BKP-20260814-0001.json", 1234L);

        assertEquals(BackupStatus.SUCCESS, record.getStatus());
        assertEquals("BKP-20260814-0001.json", record.getFileName());
        assertEquals(1234L, record.getFileSize());
        assertTrue(record.isRestorable());
    }

    @Test
    void markSuccessRejectsNegativeFileSize() {
        BackupRecord record = BackupRecord.create("BKP-20260814-0001", BackupType.FULL, null, ACTOR, NOW);

        assertThrows(ValidationException.class,
                () -> record.markSuccess("file.json", -1L));
    }

    @Test
    void markSuccessRejectsWhenAlreadySuccess() {
        BackupRecord record = BackupRecord.create("BKP-20260814-0001", BackupType.FULL, null, ACTOR, NOW);
        record.markSuccess("file.json", 10L);

        assertThrows(InvalidBackupStatusException.class,
                () -> record.markSuccess("file.json", 10L));
    }

    @Test
    void markFailedTransitionsInProgressToFailed() {
        BackupRecord record = BackupRecord.create("BKP-20260814-0001", BackupType.FULL, null, ACTOR, NOW);

        record.markFailed();

        assertEquals(BackupStatus.FAILED, record.getStatus());
        assertFalse(record.isRestorable());
    }

    @Test
    void markRestoredRejectsNonSuccessStatus() {
        BackupRecord failed = BackupRecord.restore(
                UUID.randomUUID(), "BKP-20260814-0001", null, 0L,
                BackupStatus.FAILED, BackupType.FULL, null, ACTOR, NOW, null, null);

        assertThrows(InvalidBackupStatusException.class,
                () -> failed.markRestored(ACTOR, NOW));
    }

    @Test
    void markRestoredSetsRestoreMetadata() {
        BackupRecord record = BackupRecord.create("BKP-20260814-0001", BackupType.FULL, null, ACTOR, NOW);
        record.markSuccess("file.json", 10L);
        Instant restoredAt = NOW.plusSeconds(60);

        record.markRestored(ACTOR, restoredAt);

        assertEquals(ACTOR, record.getRestoredBy());
        assertEquals(restoredAt, record.getRestoredAt());
    }
}

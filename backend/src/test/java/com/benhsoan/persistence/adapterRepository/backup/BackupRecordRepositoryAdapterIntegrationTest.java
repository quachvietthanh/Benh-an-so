package com.benhsoan.persistence.adapterRepository.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.persistence.jpaRepository.backup.JpaBackupRecordRepository;
import com.benhsoan.persistence.mapper.backup.BackupPersistenceMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:backup-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class BackupRecordRepositoryAdapterIntegrationTest {

    @Autowired
    private JpaBackupRecordRepository jpaRepository;

    private BackupRecordRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BackupRecordRepositoryAdapter(jpaRepository, new BackupPersistenceMapper());
    }

    @Test
    void roundTripsBackupRecordAndOrdersByCreatedAtDesc() {
        Instant base = Instant.parse("2026-08-14T08:00:00Z");
        UUID actor = UUID.randomUUID();

        BackupRecord first = BackupRecord.create("BKP-20260814-0001", BackupType.FULL, "first", actor, base);
        first.markSuccess("BKP-20260814-0001.json", 100L);
        adapter.save(first);

        BackupRecord second = BackupRecord.create("BKP-20260814-0002", BackupType.MANUAL, "second", actor, base.plusSeconds(60));
        second.markSuccess("BKP-20260814-0002.json", 200L);
        adapter.save(second);

        Optional<BackupRecord> found = adapter.findById(first.getId());
        assertTrue(found.isPresent());
        assertEquals(BackupStatus.SUCCESS, found.get().getStatus());
        assertEquals("BKP-20260814-0001.json", found.get().getFileName());

        var list = adapter.findAllByOrderByCreatedAtDesc();
        assertEquals(2, list.size());
        assertEquals("BKP-20260814-0002", list.get(0).getBackupCode());

        assertEquals("BKP-20260814-0002", adapter.findTopByOrderByBackupCodeDesc().orElseThrow().getBackupCode());
    }

    @Test
    void persistsInProgressBackupBeforeItsFileExists() {
        BackupRecord pending = BackupRecord.create(
                "BKP-20260814-0001", BackupType.FULL, null, UUID.randomUUID(), Instant.now());

        adapter.save(pending);

        BackupRecord saved = adapter.findById(pending.getId()).orElseThrow();
        assertEquals(BackupStatus.IN_PROGRESS, saved.getStatus());
        assertNull(saved.getFileName());
    }

    @Test
    void hasActiveInProgressBackupRespectsCutoffTime() {
        Instant now = Instant.parse("2026-09-24T10:00:00Z");
        UUID actor = UUID.randomUUID();

        // Stale record created 90 minutes ago
        BackupRecord staleRecord = BackupRecord.create(
                "BKP-STALE", BackupType.SCHEDULED, null, actor, now.minusSeconds(90 * 60));
        adapter.save(staleRecord);

        // Cutoff 60 minutes ago: stale record is BEFORE cutoff, so no active backup
        Instant cutoff = now.minusSeconds(60 * 60);
        org.junit.jupiter.api.Assertions.assertFalse(adapter.hasActiveInProgressBackup(cutoff));

        // Fresh record created 30 minutes ago
        BackupRecord freshRecord = BackupRecord.create(
                "BKP-FRESH", BackupType.SCHEDULED, null, actor, now.minusSeconds(30 * 60));
        adapter.save(freshRecord);

        // Now has active in-progress backup after cutoff
        assertTrue(adapter.hasActiveInProgressBackup(cutoff));
    }

    @Test
    void findLatestByStatusReturnsMostRecentMatchingRecord() {
        Instant base = Instant.parse("2026-09-24T10:00:00Z");
        UUID actor = UUID.randomUUID();

        BackupRecord rec1 = BackupRecord.create("BKP-01", BackupType.FULL, "first", actor, base);
        rec1.markSuccess("file1.json", 100L);
        adapter.save(rec1);

        BackupRecord rec2 = BackupRecord.create("BKP-02", BackupType.SCHEDULED, "second", actor, base.plusSeconds(60));
        rec2.markFailed("Error");
        adapter.save(rec2);

        BackupRecord rec3 = BackupRecord.create("BKP-03", BackupType.SCHEDULED, "third", actor, base.plusSeconds(120));
        rec3.markSuccess("file3.json", 300L);
        adapter.save(rec3);

        Optional<BackupRecord> latestSuccess = adapter.findLatestByStatus(BackupStatus.SUCCESS);
        assertTrue(latestSuccess.isPresent());
        assertEquals("BKP-03", latestSuccess.get().getBackupCode());

        Optional<BackupRecord> latestFailed = adapter.findLatestByStatus(BackupStatus.FAILED);
        assertTrue(latestFailed.isPresent());
        assertEquals("BKP-02", latestFailed.get().getBackupCode());
    }
}

package com.benhsoan.persistence.adapterRepository.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}

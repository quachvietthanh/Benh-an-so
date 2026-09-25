package com.benhsoan.persistence.adapterRepository.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.backup.BackupScheduleConfiguration;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.persistence.jpaRepository.backup.JpaBackupScheduleConfigurationRepository;
import com.benhsoan.persistence.mapper.backup.BackupSchedulePersistenceMapper;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:backup-schedule-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class BackupScheduleRepositoryAdapterIntegrationTest {

    @Autowired
    private JpaBackupScheduleConfigurationRepository jpaRepository;

    private BackupScheduleRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BackupScheduleRepositoryAdapter(jpaRepository, new BackupSchedulePersistenceMapper());
    }

    @Test
    void roundTripsDefaultBackupScheduleConfiguration() {
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-24T08:00:00Z");

        BackupScheduleConfiguration defaultConfig = BackupScheduleConfiguration.createDefault(adminId, now);
        adapter.save(defaultConfig);

        Optional<BackupScheduleConfiguration> found = adapter.find();
        assertTrue(found.isPresent());
        assertEquals("02:00", found.get().getDailyTime());
        assertEquals("0 0 2 * * *", found.get().getCronExpression());
        assertFalse(found.get().isEnabled());
        assertFalse(found.get().isAlertActive());
    }

    @Test
    void updatesAndPersistsScheduleAndVerificationState() {
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-24T08:00:00Z");

        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(adminId, now);
        config.updateSchedule(true, "03:30", adminId, now);
        config.recordSuccess(now.plusSeconds(3600));
        config.recordVerification(now.plusSeconds(3700), "SUCCESS");
        adapter.save(config);

        Optional<BackupScheduleConfiguration> found = adapter.find();
        assertTrue(found.isPresent());
        assertTrue(found.get().isEnabled());
        assertEquals("03:30", found.get().getDailyTime());
        assertEquals("0 30 3 * * *", found.get().getCronExpression());
        assertEquals(BackupStatus.SUCCESS, found.get().getLastStatus());
        assertEquals("SUCCESS", found.get().getLastVerificationStatus());
        assertFalse(found.get().isAlertActive());
    }
}

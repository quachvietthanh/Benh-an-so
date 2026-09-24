package com.benhsoan.domain.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.shared.exception.ValidationException;

class BackupScheduleConfigurationTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-24T02:05:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void createsDefaultConfiguration() {
        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(ACTOR, NOW);

        assertFalse(config.isEnabled());
        assertEquals("02:00", config.getDailyTime());
        assertEquals("0 0 2 * * *", config.getCronExpression());
        assertNull(config.getLastRunAt());
        assertNull(config.getLastStatus());
        assertNull(config.getLastFailureReason());
        assertFalse(config.isAlertActive());
        assertEquals(ACTOR, config.getUpdatedBy());
        assertEquals(NOW, config.getUpdatedAt());
    }

    @Test
    void updatesScheduleWithValidDailyTime() {
        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(ACTOR, NOW);
        Instant later = NOW.plusSeconds(3600);

        config.updateSchedule(true, "03:30", ACTOR, later);

        assertTrue(config.isEnabled());
        assertEquals("03:30", config.getDailyTime());
        assertEquals("0 30 3 * * *", config.getCronExpression());
        assertEquals(later, config.getUpdatedAt());
    }

    @Test
    void rejectsInvalidDailyTime() {
        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(ACTOR, NOW);

        assertThrows(ValidationException.class, () -> config.updateSchedule(true, "25:00", ACTOR, NOW));
        assertThrows(ValidationException.class, () -> config.updateSchedule(true, "12:60", ACTOR, NOW));
        assertThrows(ValidationException.class, () -> config.updateSchedule(true, "invalid", ACTOR, NOW));
        assertThrows(ValidationException.class, () -> config.updateSchedule(true, null, ACTOR, NOW));
    }

    @Test
    void recordsSuccessClearsAlertAndUpdatesStatus() {
        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(ACTOR, NOW);
        config.recordFailure(NOW.minusSeconds(100), "Network timeout");
        assertTrue(config.isAlertActive());

        config.recordSuccess(NOW);

        assertFalse(config.isAlertActive());
        assertEquals(BackupStatus.SUCCESS, config.getLastStatus());
        assertNull(config.getLastFailureReason());
        assertEquals(NOW, config.getLastRunAt());
    }

    @Test
    void recordsFailureActivatesAlert() {
        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(ACTOR, NOW);

        config.recordFailure(NOW, "Disk space full");

        assertTrue(config.isAlertActive());
        assertEquals(BackupStatus.FAILED, config.getLastStatus());
        assertEquals("Disk space full", config.getLastFailureReason());
        assertEquals(NOW, config.getLastRunAt());
    }

    @Test
    void dismissAlertDeactivatesAlertFlag() {
        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(ACTOR, NOW);
        config.recordFailure(NOW, "Disk space full");
        assertTrue(config.isAlertActive());

        config.dismissAlert(ACTOR, NOW.plusSeconds(60));

        assertFalse(config.isAlertActive());
        assertEquals(BackupStatus.FAILED, config.getLastStatus());
        assertEquals("Disk space full", config.getLastFailureReason());
    }

    @Test
    void isDueReturnsFalseWhenDisabled() {
        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(ACTOR, NOW);
        assertFalse(config.isEnabled());

        assertFalse(config.isDue(NOW, ZONE));
    }

    @Test
    void isDueReturnsTrueWhenEnabledAndPastScheduledTimeToday() {
        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(ACTOR, NOW);
        // Set daily time to 09:00 in Vietnam time (+07:00)
        config.updateSchedule(true, "09:00", ACTOR, NOW);

        // 2026-09-24T02:05:00Z is 09:05:00 in Asia/Ho_Chi_Minh (+7)
        Instant at905Vietnam = Instant.parse("2026-09-24T02:05:00Z");
        assertTrue(config.isDue(at905Vietnam, ZONE));
    }

    @Test
    void isDueReturnsFalseWhenBeforeScheduledTimeToday() {
        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(ACTOR, NOW);
        // Set daily time to 09:00 in Vietnam time (+07:00)
        config.updateSchedule(true, "09:00", ACTOR, NOW);

        // 2026-09-24T01:55:00Z is 08:55:00 in Asia/Ho_Chi_Minh (+7)
        Instant at855Vietnam = Instant.parse("2026-09-24T01:55:00Z");
        assertFalse(config.isDue(at855Vietnam, ZONE));
    }

    @Test
    void isDueReturnsFalseWhenAlreadyRunTodayAfterScheduledTime() {
        BackupScheduleConfiguration config = BackupScheduleConfiguration.createDefault(ACTOR, NOW);
        config.updateSchedule(true, "09:00", ACTOR, NOW);

        // 09:05:00 today
        Instant at905Vietnam = Instant.parse("2026-09-24T02:05:00Z");
        config.recordSuccess(at905Vietnam);

        // Later today at 10:00 (03:00Z)
        Instant at1000Vietnam = Instant.parse("2026-09-24T03:00:00Z");
        assertFalse(config.isDue(at1000Vietnam, ZONE));
    }
}

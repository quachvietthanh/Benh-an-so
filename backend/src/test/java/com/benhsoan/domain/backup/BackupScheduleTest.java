package com.benhsoan.domain.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class BackupScheduleTest {

    private static final Instant NOW = Instant.parse("2026-08-14T08:00:00Z");

    @Test
    void createDefaultIsDisabledWithDefaultTime() {
        BackupSchedule schedule = BackupSchedule.createDefault(NOW);

        assertFalse(schedule.isEnabled());
        assertEquals(LocalTime.of(2, 0), schedule.getBackupTime());
        assertEquals(BackupSchedule.SINGLETON_ID, schedule.getId());
        assertEquals(NOW, schedule.getUpdatedAt());
    }

    @Test
    void createStoresEnabledAndTime() {
        BackupSchedule schedule = BackupSchedule.create(true, LocalTime.of(3, 30), NOW);

        assertTrue(schedule.isEnabled());
        assertEquals(LocalTime.of(3, 30), schedule.getBackupTime());
    }

    @Test
    void updateChangesEnabledAndTime() {
        BackupSchedule schedule = BackupSchedule.create(false, LocalTime.of(2, 0), NOW);
        Instant later = NOW.plusSeconds(60);

        schedule.update(true, LocalTime.of(4, 15), later);

        assertTrue(schedule.isEnabled());
        assertEquals(LocalTime.of(4, 15), schedule.getBackupTime());
        assertEquals(later, schedule.getUpdatedAt());
    }

    @Test
    void rejectsNullBackupTime() {
        assertThrows(ValidationException.class,
                () -> BackupSchedule.create(true, null, NOW));
    }

    @Test
    void rejectsWrongId() {
        assertThrows(ValidationException.class,
                () -> BackupSchedule.restore(2, false, LocalTime.NOON, NOW, NOW));
    }
}

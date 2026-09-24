package com.benhsoan.domain.backup;

import java.time.Instant;
import java.time.LocalTime;

import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Singleton configuration for the automatic daily backup schedule (NCL-09-CN-009).
 * The fixed primary key enforces a single schedule per system, mirroring
 * {@code clinic_configuration}.
 */
@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BackupSchedule {

    public static final int SINGLETON_ID = 1;

    public static final LocalTime DEFAULT_BACKUP_TIME = LocalTime.of(2, 0);

    private final int id;
    private boolean enabled;
    private LocalTime backupTime;
    private final Instant createdAt;
    private Instant updatedAt;

    private BackupSchedule(int id, boolean enabled, LocalTime backupTime, Instant createdAt, Instant updatedAt) {
        if (id != SINGLETON_ID) {
            throw new ValidationException("Backup schedule id must be " + SINGLETON_ID + ".");
        }
        this.id = id;
        this.enabled = enabled;
        this.backupTime = Guard.require(backupTime, "Backup time");
        this.createdAt = Guard.require(createdAt, "Created at");
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public static BackupSchedule createDefault(Instant now) {
        return new BackupSchedule(SINGLETON_ID, false, DEFAULT_BACKUP_TIME, now, now);
    }

    public static BackupSchedule create(boolean enabled, LocalTime backupTime, Instant now) {
        return new BackupSchedule(SINGLETON_ID, enabled, backupTime, now, now);
    }

    public static BackupSchedule restore(
            int id,
            boolean enabled,
            LocalTime backupTime,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new BackupSchedule(id, enabled, backupTime, createdAt, updatedAt);
    }

    public void update(boolean enabled, LocalTime backupTime, Instant updatedAt) {
        this.enabled = enabled;
        this.backupTime = Guard.require(backupTime, "Backup time");
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }
}

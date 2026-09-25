package com.benhsoan.domain.backup;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BackupScheduleConfiguration {

    private static final Pattern DAILY_TIME_PATTERN = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");
    private static final String DEFAULT_DAILY_TIME = "02:00";

    private UUID id;
    private boolean enabled;
    private String dailyTime;
    private String cronExpression;
    private Instant lastRunAt;
    private BackupStatus lastStatus;
    private String lastFailureReason;
    private boolean alertActive;
    private Instant lastVerifiedAt;
    private String lastVerificationStatus;
    private UUID updatedBy;
    private Instant updatedAt;

    private BackupScheduleConfiguration(
            UUID id,
            boolean enabled,
            String dailyTime,
            String cronExpression,
            Instant lastRunAt,
            BackupStatus lastStatus,
            String lastFailureReason,
            boolean alertActive,
            Instant lastVerifiedAt,
            String lastVerificationStatus,
            UUID updatedBy,
            Instant updatedAt
    ) {
        this.id = Guard.require(id, "Id");
        this.enabled = enabled;
        this.dailyTime = validateDailyTime(dailyTime);
        this.cronExpression = cronExpression != null ? cronExpression : toCronExpression(this.dailyTime);
        this.lastRunAt = lastRunAt;
        this.lastStatus = lastStatus;
        this.lastFailureReason = lastFailureReason;
        this.alertActive = alertActive;
        this.lastVerifiedAt = lastVerifiedAt;
        this.lastVerificationStatus = lastVerificationStatus;
        this.updatedBy = Guard.require(updatedBy, "Updated by");
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public static BackupScheduleConfiguration createDefault(UUID updatedBy, Instant now) {
        return new BackupScheduleConfiguration(
                UUID.randomUUID(),
                false,
                DEFAULT_DAILY_TIME,
                toCronExpression(DEFAULT_DAILY_TIME),
                null,
                null,
                null,
                false,
                null,
                null,
                updatedBy,
                now
        );
    }

    public static BackupScheduleConfiguration restore(
            UUID id,
            boolean enabled,
            String dailyTime,
            String cronExpression,
            Instant lastRunAt,
            BackupStatus lastStatus,
            String lastFailureReason,
            boolean alertActive,
            Instant lastVerifiedAt,
            String lastVerificationStatus,
            UUID updatedBy,
            Instant updatedAt
    ) {
        return new BackupScheduleConfiguration(
                id,
                enabled,
                dailyTime,
                cronExpression,
                lastRunAt,
                lastStatus,
                lastFailureReason,
                alertActive,
                lastVerifiedAt,
                lastVerificationStatus,
                updatedBy,
                updatedAt
        );
    }

    public void updateSchedule(boolean enabled, String dailyTime, UUID updatedBy, Instant updatedAt) {
        this.enabled = enabled;
        this.dailyTime = validateDailyTime(dailyTime);
        this.cronExpression = toCronExpression(this.dailyTime);
        this.updatedBy = Guard.require(updatedBy, "Updated by");
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public void recordSuccess(Instant runAt) {
        this.lastRunAt = Guard.require(runAt, "Run at");
        this.lastStatus = BackupStatus.SUCCESS;
        this.lastFailureReason = null;
        this.alertActive = false;
        this.updatedAt = runAt;
    }

    public void recordFailure(Instant runAt, String reason) {
        this.lastRunAt = Guard.require(runAt, "Run at");
        this.lastStatus = BackupStatus.FAILED;
        this.lastFailureReason = reason;
        this.alertActive = true;
        this.updatedAt = runAt;
    }

    public void dismissAlert(UUID dismissedBy, Instant dismissedAt) {
        this.alertActive = false;
        this.updatedBy = Guard.require(dismissedBy, "Dismissed by");
        this.updatedAt = Guard.require(dismissedAt, "Dismissed at");
    }

    public void recordVerification(Instant verifiedAt, String status) {
        this.lastVerifiedAt = Guard.require(verifiedAt, "Verified at");
        this.lastVerificationStatus = Guard.require(status, "Verification status");
    }

    public boolean isDue(Instant now, ZoneId zoneId) {
        if (!enabled) {
            return false;
        }
        ZoneId zone = zoneId != null ? zoneId : ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime zdtNow = now.atZone(zone);
        LocalTime scheduledTime = LocalTime.parse(dailyTime);
        LocalDate today = zdtNow.toLocalDate();
        ZonedDateTime scheduledToday = today.atTime(scheduledTime).atZone(zone);

        if (zdtNow.isBefore(scheduledToday)) {
            return false;
        }

        if (lastRunAt != null) {
            ZonedDateTime lastRunZdt = lastRunAt.atZone(zone);
            if (!lastRunZdt.isBefore(scheduledToday)) {
                return false;
            }
        }

        return true;
    }

    private static String validateDailyTime(String dailyTime) {
        if (dailyTime == null || !DAILY_TIME_PATTERN.matcher(dailyTime.trim()).matches()) {
            throw new ValidationException("Daily time must be in HH:mm format (00:00 to 23:59).");
        }
        String[] parts = dailyTime.trim().split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return String.format("%02d:%02d", hour, minute);
    }

    private static String toCronExpression(String dailyTime) {
        String[] parts = dailyTime.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return String.format("0 %d %d * * *", minute, hour);
    }
}

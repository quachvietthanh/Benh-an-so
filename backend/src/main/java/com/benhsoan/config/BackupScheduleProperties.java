package com.benhsoan.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the automatic backup schedule (NCL-09-CN-009). The zone id
 * determines which local date/time a daily {@code backupTime} is evaluated
 * against.
 */
@ConfigurationProperties(prefix = "app.backup.schedule")
public record BackupScheduleProperties(
        ZoneId zoneId
) {
}

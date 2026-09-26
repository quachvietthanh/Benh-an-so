package com.benhsoan.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "appointment.reminder")
public record AppointmentReminderProperties(
        boolean enabled,
        long advanceHours,
        long scanIntervalMs,
        ZoneId zoneId
) {
}

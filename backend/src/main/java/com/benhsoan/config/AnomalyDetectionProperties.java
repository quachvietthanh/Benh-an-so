package com.benhsoan.config;

import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.anomaly")
public record AnomalyDetectionProperties(
        int maxViewsPerHour,
        LocalTime workingHoursStart,
        LocalTime workingHoursEnd,
        ZoneId zoneId
) {
}

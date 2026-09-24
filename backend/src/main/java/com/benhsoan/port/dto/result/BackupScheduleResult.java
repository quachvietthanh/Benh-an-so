package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalTime;

public record BackupScheduleResult(
        boolean enabled,
        LocalTime backupTime,
        Instant updatedAt
) {
}

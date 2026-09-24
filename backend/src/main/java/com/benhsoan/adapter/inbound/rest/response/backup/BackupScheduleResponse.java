package com.benhsoan.adapter.inbound.rest.response.backup;

import java.time.Instant;
import java.time.LocalTime;

public record BackupScheduleResponse(
        boolean enabled,
        LocalTime backupTime,
        Instant updatedAt
) {
}

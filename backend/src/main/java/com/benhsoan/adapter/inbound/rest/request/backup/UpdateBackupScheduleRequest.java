package com.benhsoan.adapter.inbound.rest.request.backup;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record UpdateBackupScheduleRequest(
        @NotNull(message = "Enabled is required.")
        Boolean enabled,

        @NotNull(message = "Backup time is required.")
        LocalTime backupTime
) {
}

package com.benhsoan.port.dto.command.backup;

import java.time.LocalTime;

/**
 * Full-state update of the automatic backup schedule. Both fields are required:
 * {@code PUT} semantics replace the complete schedule state.
 */
public record UpdateBackupScheduleCommand(
        boolean enabled,
        LocalTime backupTime
) {
}

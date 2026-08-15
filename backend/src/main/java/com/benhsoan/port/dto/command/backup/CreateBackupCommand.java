package com.benhsoan.port.dto.command.backup;

import com.benhsoan.domain.backup.enums.BackupType;

public record CreateBackupCommand(
        BackupType backupType,
        String description
) {
}

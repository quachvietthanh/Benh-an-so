package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;

public record BackupResult(
        UUID id,
        String backupCode,
        String fileName,
        long fileSize,
        BackupStatus status,
        BackupType backupType,
        String description,
        UUID createdBy,
        Instant createdAt,
        Instant restoredAt,
        UUID restoredBy
) {
}

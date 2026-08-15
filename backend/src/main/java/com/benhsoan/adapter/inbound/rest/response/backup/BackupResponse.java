package com.benhsoan.adapter.inbound.rest.response.backup;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;

public record BackupResponse(
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

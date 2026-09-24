package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record BackupIntegrityResult(
        UUID backupId,
        String backupCode,
        String fileName,
        Instant backupCreatedAt,
        boolean valid,
        String reason,
        int tableCount,
        long rowCount,
        Instant verifiedAt
) {
}

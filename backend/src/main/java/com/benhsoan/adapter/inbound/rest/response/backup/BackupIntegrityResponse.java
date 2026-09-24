package com.benhsoan.adapter.inbound.rest.response.backup;

import java.time.Instant;
import java.util.UUID;

public record BackupIntegrityResponse(
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

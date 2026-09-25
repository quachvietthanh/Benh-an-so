package com.benhsoan.adapter.inbound.rest.response.backup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BackupVerificationResponse(
        UUID backupId,
        String backupCode,
        String fileName,
        boolean valid,
        boolean readable,
        boolean dataIntact,
        int tableCount,
        int rowCount,
        String schemaVersion,
        Instant verifiedAt,
        String message,
        List<String> issues
) {
}

package com.benhsoan.domain.backup;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record BackupVerificationReport(
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
    public BackupVerificationReport {
        issues = issues != null ? Collections.unmodifiableList(issues) : Collections.emptyList();
    }

    public static BackupVerificationReport success(
            UUID backupId,
            String backupCode,
            String fileName,
            int tableCount,
            int rowCount,
            String schemaVersion,
            Instant verifiedAt
    ) {
        return new BackupVerificationReport(
                backupId,
                backupCode,
                fileName,
                true,
                true,
                true,
                tableCount,
                rowCount,
                schemaVersion,
                verifiedAt,
                "Bản sao lưu đọc được và đủ dữ liệu.",
                Collections.emptyList()
        );
    }

    public static BackupVerificationReport failure(
            UUID backupId,
            String backupCode,
            String fileName,
            boolean readable,
            int tableCount,
            int rowCount,
            String schemaVersion,
            Instant verifiedAt,
            String message,
            List<String> issues
    ) {
        return new BackupVerificationReport(
                backupId,
                backupCode,
                fileName,
                false,
                readable,
                false,
                tableCount,
                rowCount,
                schemaVersion,
                verifiedAt,
                message != null ? message : "Kiểm tra tính toàn vẹn bản sao lưu thất bại.",
                issues != null ? issues : Collections.emptyList()
        );
    }
}

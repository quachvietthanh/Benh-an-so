package com.benhsoan.port.outbound.backup;

/**
 * Result of an integrity verification of a stored backup snapshot. Verification
 * is read-only and never mutates the backup content or its metadata.
 */
public record BackupVerification(
        boolean valid,
        String reason,
        int tableCount,
        long rowCount
) {

    public static BackupVerification valid(int tableCount, long rowCount) {
        return new BackupVerification(true, null, tableCount, rowCount);
    }

    public static BackupVerification invalid(String reason) {
        return new BackupVerification(false, reason, 0, 0L);
    }
}

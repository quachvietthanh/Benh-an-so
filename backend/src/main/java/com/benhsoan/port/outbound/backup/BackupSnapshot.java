package com.benhsoan.port.outbound.backup;

public record BackupSnapshot(
        String fileName,
        byte[] content
) {
}

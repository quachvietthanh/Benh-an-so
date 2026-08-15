package com.benhsoan.port.dto.result;

import java.util.UUID;

public record BackupDownloadResult(
        UUID id,
        String fileName,
        String contentType,
        byte[] content
) {
}

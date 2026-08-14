package com.benhsoan.application.ucservice.backup;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.port.dto.result.BackupResult;

@Component
public class BackupResultMapper {

    public BackupResult toResult(BackupRecord record) {
        if (record == null) {
            return null;
        }
        return new BackupResult(
                record.getId(),
                record.getBackupCode(),
                record.getFileName(),
                record.getFileSize(),
                record.getStatus(),
                record.getBackupType(),
                record.getDescription(),
                record.getCreatedBy(),
                record.getCreatedAt(),
                record.getRestoredAt(),
                record.getRestoredBy()
        );
    }
}

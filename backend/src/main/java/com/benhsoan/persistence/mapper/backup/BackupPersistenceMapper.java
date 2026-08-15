package com.benhsoan.persistence.mapper.backup;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.persistence.entity.backup.BackupRecordEntity;

@Component
public class BackupPersistenceMapper {

    public BackupRecord toDomain(BackupRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        return BackupRecord.restore(
                entity.getId(),
                entity.getBackupCode(),
                entity.getFileName(),
                entity.getFileSize(),
                entity.getStatus(),
                entity.getBackupType(),
                entity.getDescription(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getRestoredAt(),
                entity.getRestoredBy()
        );
    }

    public BackupRecordEntity toEntity(BackupRecord domain) {
        if (domain == null) {
            return null;
        }
        return BackupRecordEntity.builder()
                .id(domain.getId())
                .backupCode(domain.getBackupCode())
                .fileName(domain.getFileName())
                .fileSize(domain.getFileSize())
                .status(domain.getStatus())
                .backupType(domain.getBackupType())
                .description(domain.getDescription())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .restoredAt(domain.getRestoredAt())
                .restoredBy(domain.getRestoredBy())
                .build();
    }
}

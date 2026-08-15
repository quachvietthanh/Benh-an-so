package com.benhsoan.domain.backup;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.domain.backup.exception.InvalidBackupStatusException;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BackupRecord {

    private UUID id;
    private String backupCode;
    private String fileName;
    private long fileSize;
    private BackupStatus status;
    private BackupType backupType;
    private String description;
    private UUID createdBy;
    private Instant createdAt;
    private Instant restoredAt;
    private UUID restoredBy;

    private BackupRecord(
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
        this.id = Guard.require(id, "Backup id");
        this.backupCode = Guard.require(backupCode, "Backup code");
        this.status = Guard.require(status, "Backup status");
        this.backupType = Guard.require(backupType, "Backup type");
        this.createdBy = Guard.require(createdBy, "Created by");
        this.createdAt = Guard.require(createdAt, "Created at");
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.description = description;
        this.restoredAt = restoredAt;
        this.restoredBy = restoredBy;
    }

    public static BackupRecord create(
            String backupCode,
            BackupType backupType,
            String description,
            UUID createdBy,
            Instant createdAt
    ) {
        return new BackupRecord(
                UUID.randomUUID(),
                backupCode,
                null,
                0L,
                BackupStatus.IN_PROGRESS,
                backupType,
                description,
                createdBy,
                createdAt,
                null,
                null
        );
    }

    public static BackupRecord restore(
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
        return new BackupRecord(
                id,
                backupCode,
                fileName,
                fileSize,
                status,
                backupType,
                description,
                createdBy,
                createdAt,
                restoredAt,
                restoredBy
        );
    }

    public void markSuccess(String fileName, long fileSize) {
        if (status != BackupStatus.IN_PROGRESS) {
            throw new InvalidBackupStatusException("Only in-progress backups can be marked successful.");
        }
        this.fileName = Guard.require(fileName, "File name");
        if (fileSize < 0) {
            throw new ValidationException("File size must not be negative.");
        }
        this.fileSize = fileSize;
        this.status = BackupStatus.SUCCESS;
    }

    public void markFailed() {
        if (status != BackupStatus.IN_PROGRESS) {
            throw new InvalidBackupStatusException("Only in-progress backups can be marked failed.");
        }
        this.status = BackupStatus.FAILED;
    }

    public void markRestored(UUID restoredBy, Instant restoredAt) {
        if (status != BackupStatus.SUCCESS) {
            throw new InvalidBackupStatusException(status);
        }
        this.restoredBy = Guard.require(restoredBy, "Restored by");
        this.restoredAt = Guard.require(restoredAt, "Restored at");
    }

    public boolean isRestorable() {
        return status == BackupStatus.SUCCESS;
    }
}

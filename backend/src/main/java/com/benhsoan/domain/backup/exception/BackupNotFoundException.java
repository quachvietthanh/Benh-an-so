package com.benhsoan.domain.backup.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class BackupNotFoundException extends BackupException {

    public BackupNotFoundException(UUID backupId) {
        super(DomainErrorCode.BACKUP_NOT_FOUND, "Backup not found: " + backupId);
    }
}

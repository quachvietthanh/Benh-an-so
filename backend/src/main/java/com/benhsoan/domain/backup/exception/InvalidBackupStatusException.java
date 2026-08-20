package com.benhsoan.domain.backup.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.backup.enums.BackupStatus;
public class InvalidBackupStatusException extends BackupException {

    public InvalidBackupStatusException(BackupStatus status) {
        super(DomainErrorCode.INVALID_BACKUP_STATUS, "Backup is not restorable because its status is " + status + ".");
    }

    public InvalidBackupStatusException(String message) {
        super(DomainErrorCode.INVALID_BACKUP_STATUS, message);
    }
}

package com.benhsoan.domain.backup.exception;


import com.benhsoan.domain.backup.enums.BackupStatus;
public class InvalidBackupStatusException extends BackupException {

    public InvalidBackupStatusException(BackupStatus status) {
        super("Backup is not restorable because its status is " + status + ".");
    }

    public InvalidBackupStatusException(String message) {
        super(message);
    }
}

package com.benhsoan.domain.backup.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.shared.exception.DomainException;

public class InvalidBackupStatusException extends DomainException {

    public InvalidBackupStatusException(BackupStatus status) {
        super(HttpStatus.BAD_REQUEST, "Backup is not restorable because its status is " + status + ".");
    }

    public InvalidBackupStatusException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}

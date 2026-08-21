package com.benhsoan.domain.backup.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class BackupExecutionException extends BackupException {

    public BackupExecutionException(String message) {
        super(DomainErrorCode.BACKUP_EXECUTION_FAILED, message);
    }
}

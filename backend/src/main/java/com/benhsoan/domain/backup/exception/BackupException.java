package com.benhsoan.domain.backup.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class BackupException extends DomainException {

    protected BackupException(String message) {
        super(message);
    }
}

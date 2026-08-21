package com.benhsoan.domain.backup.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class BackupException extends DomainException {

    protected BackupException(DomainErrorCode code, String message) {
        super(code, message);
    }
}

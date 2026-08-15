package com.benhsoan.domain.backup.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class BackupExecutionException extends DomainException {

    public BackupExecutionException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}

package com.benhsoan.domain.backup.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class BackupNotFoundException extends DomainException {

    public BackupNotFoundException(UUID backupId) {
        super(HttpStatus.NOT_FOUND, "Backup not found: " + backupId);
    }
}

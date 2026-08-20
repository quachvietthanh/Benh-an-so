package com.benhsoan.domain.backup.exception;

import java.util.UUID;


public class BackupNotFoundException extends BackupException {

    public BackupNotFoundException(UUID backupId) {
        super("Backup not found: " + backupId);
    }
}

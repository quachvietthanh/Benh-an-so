package com.benhsoan.port.inbound.backup;

import java.util.UUID;

import com.benhsoan.port.dto.result.BackupResult;

public interface RestoreBackupUseCase {

    BackupResult restore(UUID backupId);
}

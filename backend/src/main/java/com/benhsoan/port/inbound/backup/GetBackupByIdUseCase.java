package com.benhsoan.port.inbound.backup;

import java.util.UUID;

import com.benhsoan.port.dto.result.BackupResult;

public interface GetBackupByIdUseCase {

    BackupResult getById(UUID backupId);
}

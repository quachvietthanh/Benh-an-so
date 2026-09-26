package com.benhsoan.port.inbound.backup;

import com.benhsoan.port.dto.command.backup.CreateBackupCommand;
import com.benhsoan.port.dto.result.BackupResult;

public interface CreateBackupUseCase {

    BackupResult create(CreateBackupCommand command);
}

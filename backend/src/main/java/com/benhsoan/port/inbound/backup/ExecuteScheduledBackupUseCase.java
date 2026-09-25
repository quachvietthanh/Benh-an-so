package com.benhsoan.port.inbound.backup;

import com.benhsoan.port.dto.result.BackupResult;

public interface ExecuteScheduledBackupUseCase {

    void executeIfDue();

    BackupResult executeNow();
}

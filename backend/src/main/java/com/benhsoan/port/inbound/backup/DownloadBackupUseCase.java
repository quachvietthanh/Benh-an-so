package com.benhsoan.port.inbound.backup;

import java.util.UUID;

import com.benhsoan.port.dto.result.BackupDownloadResult;

public interface DownloadBackupUseCase {

    BackupDownloadResult download(UUID backupId);
}

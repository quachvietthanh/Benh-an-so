package com.benhsoan.port.inbound.backup;

import com.benhsoan.port.dto.result.BackupIntegrityResult;

public interface VerifyLatestBackupUseCase {

    BackupIntegrityResult verifyLatest();
}

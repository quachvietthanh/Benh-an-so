package com.benhsoan.port.inbound.backup;

import java.util.UUID;

import com.benhsoan.domain.backup.BackupVerificationReport;

public interface VerifyBackupIntegrityUseCase {

    BackupVerificationReport verifyLatest();

    BackupVerificationReport verifyById(UUID backupId);
}

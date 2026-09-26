package com.benhsoan.application.ucservice.backup;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class BackupSnapshotExportService {

    private final DatabaseBackupStoragePort storagePort;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRES_NEW)
    public BackupSnapshot export(String backupCode) {
        return storagePort.exportSnapshot(backupCode);
    }
}

package com.benhsoan.port.outbound.backup;

public interface DatabaseBackupStoragePort {

    BackupSnapshot exportSnapshot(String backupCode);

    BackupSnapshot loadSnapshot(String fileName);

    void restoreSnapshot(String fileName);
}

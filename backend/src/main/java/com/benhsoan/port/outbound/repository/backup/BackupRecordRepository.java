package com.benhsoan.port.outbound.repository.backup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.backup.BackupRecord;

public interface BackupRecordRepository {

    BackupRecord save(BackupRecord record);

    Optional<BackupRecord> findById(UUID id);

    List<BackupRecord> findAllByOrderByCreatedAtDesc();

    Optional<BackupRecord> findTopByOrderByBackupCodeDesc();
}

package com.benhsoan.application.ucservice.backup;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.exception.BackupNotFoundException;
import com.benhsoan.port.dto.result.BackupDownloadResult;
import com.benhsoan.port.inbound.backup.DownloadBackupUseCase;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DownloadBackupService implements DownloadBackupUseCase {

    private static final String CONTENT_TYPE = "application/json";

    private final BackupRecordRepository backupRecordRepository;
    private final DatabaseBackupStoragePort storagePort;
    private final BackupAuthorizer authorizer;

    @Override
    public BackupDownloadResult download(UUID backupId) {
        authorizer.requireAdmin();

        BackupRecord record = backupRecordRepository.findById(backupId)
                .orElseThrow(() -> new BackupNotFoundException(backupId));

        BackupSnapshot snapshot = storagePort.loadSnapshot(record.getFileName());

        return new BackupDownloadResult(record.getId(), snapshot.fileName(), CONTENT_TYPE, snapshot.content());
    }
}

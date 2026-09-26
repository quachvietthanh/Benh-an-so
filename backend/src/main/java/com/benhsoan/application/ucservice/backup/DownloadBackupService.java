package com.benhsoan.application.ucservice.backup;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.exception.BackupNotFoundException;
import com.benhsoan.port.dto.result.BackupDownloadResult;
import com.benhsoan.port.inbound.backup.DownloadBackupUseCase;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DownloadBackupService implements DownloadBackupUseCase {

    private static final String CONTENT_TYPE = "application/json";

    private final BackupRecordRepository backupRecordRepository;
    private final DatabaseBackupStoragePort storagePort;
    private final BackupAuthorizer authorizer;
    private final AdminOperationAuditService adminOperationAuditService;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public BackupDownloadResult download(UUID backupId) {
        authorizer.requireAdmin();

        BackupRecord record = backupRecordRepository.findById(backupId)
                .orElseThrow(() -> new BackupNotFoundException(backupId));

        BackupSnapshot snapshot = storagePort.loadSnapshot(record.getFileName());

        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.EXPORT,
                ResourceType.SYSTEM_BACKUP,
                record.getId(),
                null,
                AdminOperationAuditService.fields(
                        "backupCode", record.getBackupCode(),
                        "fileName", snapshot.fileName(),
                        "fileSize", snapshot.content() != null ? snapshot.content().length : record.getFileSize()
                ),
                clockPort.now()
        );

        return new BackupDownloadResult(record.getId(), snapshot.fileName(), CONTENT_TYPE, snapshot.content());
    }
}

package com.benhsoan.application.ucservice.backup;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.domain.backup.exception.BackupExecutionException;
import com.benhsoan.port.dto.command.backup.CreateBackupCommand;
import com.benhsoan.port.dto.result.BackupResult;
import com.benhsoan.port.inbound.backup.CreateBackupUseCase;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateBackupService implements CreateBackupUseCase {

    private final BackupRecordLifecycleService lifecycleService;
    private final BackupSnapshotExportService snapshotExportService;
    private final BackupCodeGenerator backupCodeGenerator;
    private final BackupAuditLogWriter auditLogWriter;
    private final BackupResultMapper resultMapper;
    private final BackupAuthorizer authorizer;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public BackupResult create(CreateBackupCommand command) {
        authorizer.requireAdmin();

        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        BackupType type = command == null || command.backupType() == null
                ? BackupType.FULL
                : command.backupType();
        String description = command == null ? null : command.description();

        String backupCode = backupCodeGenerator.generate();
        BackupRecord record = lifecycleService.createInProgress(backupCode, type, description, actorId, now);

        try {
            BackupSnapshot snapshot = snapshotExportService.export(backupCode);
            record = lifecycleService.markSuccess(record.getId(), snapshot);
        } catch (RuntimeException ex) {
            lifecycleService.markFailed(record.getId());
            throw new BackupExecutionException("Failed to create backup snapshot: " + ex.getMessage());
        }

        auditLogWriter.write(actorId, ActionType.BACKUP, record.getId(), toDetail(record));

        return resultMapper.toResult(record);
    }

    private String toDetail(BackupRecord record) {
        return """
                {
                "backupCode":"%s",
                "fileName":"%s",
                "fileSize":%d,
                "backupType":"%s"
                }
                """.formatted(
                record.getBackupCode(),
                record.getFileName(),
                record.getFileSize(),
                record.getBackupType()
        );
    }
}

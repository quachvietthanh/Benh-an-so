package com.benhsoan.application.ucservice.backup;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.domain.backup.exception.BackupNotFoundException;
import com.benhsoan.domain.backup.exception.InvalidBackupStatusException;
import com.benhsoan.port.dto.result.BackupResult;
import com.benhsoan.port.inbound.backup.RestoreBackupUseCase;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RestoreBackupService implements RestoreBackupUseCase {

    private final BackupRecordRepository backupRecordRepository;
    private final DatabaseBackupStoragePort storagePort;
    private final BackupAuditLogWriter auditLogWriter;
    private final BackupResultMapper resultMapper;
    private final BackupAuthorizer authorizer;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public BackupResult restore(UUID backupId) {
        authorizer.requireAdmin();

        BackupRecord record = backupRecordRepository.findById(backupId)
                .orElseThrow(() -> new BackupNotFoundException(backupId));

        if (record.getStatus() != BackupStatus.SUCCESS) {
            throw new InvalidBackupStatusException(record.getStatus());
        }

        storagePort.restoreSnapshot(record.getFileName());

        UUID actorId = currentUserPort.getCurrentUserId();
        record.markRestored(actorId, clockPort.now());
        record = backupRecordRepository.save(record);

        auditLogWriter.write(actorId, ActionType.RESTORE, record.getId(), toDetail(record));

        return resultMapper.toResult(record);
    }

    private String toDetail(BackupRecord record) {
        return """
                {
                "backupCode":"%s",
                "fileName":"%s",
                "restoredAt":"%s"
                }
                """.formatted(
                record.getBackupCode(),
                record.getFileName(),
                record.getRestoredAt()
        );
    }
}

package com.benhsoan.application.ucservice.backup;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.enums.BackupStatus;
import com.benhsoan.port.dto.result.BackupIntegrityResult;
import com.benhsoan.port.inbound.backup.VerifyLatestBackupUseCase;
import com.benhsoan.port.outbound.backup.BackupVerification;
import com.benhsoan.port.outbound.backup.DatabaseBackupStoragePort;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * Verifies the integrity of the most recent successful backup by reusing the
 * exact validation used by restore ({@code validateSnapshot}). It is read-only
 * with respect to backup content and never marks a failed/incomplete backup as
 * valid.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerifyLatestBackupService implements VerifyLatestBackupUseCase {

    private static final String NO_BACKUP_REASON = "No successful backup found.";

    private final BackupRecordRepository backupRecordRepository;
    private final DatabaseBackupStoragePort storagePort;
    private final BackupAuthorizer authorizer;
    private final BackupAuditLogWriter auditLogWriter;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public BackupIntegrityResult verifyLatest() {
        authorizer.requireAdmin();

        Instant now = clockPort.now();
        BackupRecord latest = backupRecordRepository
                .findTopByStatusOrderByCreatedAtDesc(BackupStatus.SUCCESS)
                .orElse(null);

        if (latest == null) {
            return new BackupIntegrityResult(null, null, null, null, false, NO_BACKUP_REASON, 0, 0L, now);
        }

        BackupVerification verification = storagePort.verifySnapshot(latest.getFileName());

        auditLogWriter.write(
                currentUserPort.getCurrentUserId(),
                ActionType.BACKUP_VERIFY,
                latest.getId(),
                "{\"backupCode\":\"" + latest.getBackupCode() + "\","
                        + "\"valid\":" + verification.valid() + ","
                        + "\"reason\":" + (verification.reason() == null
                                ? "null"
                                : "\"" + verification.reason().replace("\"", "'") + "\"") + "}"
        );

        return new BackupIntegrityResult(
                latest.getId(),
                latest.getBackupCode(),
                latest.getFileName(),
                latest.getCreatedAt(),
                verification.valid(),
                verification.reason(),
                verification.tableCount(),
                verification.rowCount(),
                now
        );
    }
}

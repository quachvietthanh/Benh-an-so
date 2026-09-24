package com.benhsoan.application.ucservice.backup;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.benhsoan.config.BackupScheduleProperties;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.backup.BackupRecord;
import com.benhsoan.domain.backup.BackupSchedule;
import com.benhsoan.domain.backup.enums.BackupType;
import com.benhsoan.port.inbound.backup.ExecuteScheduledBackupUseCase;
import com.benhsoan.port.outbound.backup.BackupSnapshot;
import com.benhsoan.port.outbound.repository.backup.BackupRecordRepository;
import com.benhsoan.port.outbound.repository.backup.BackupScheduleRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * Executes the automatic daily backup by delegating to the existing
 * {@link BackupSnapshotExportService} and {@link BackupRecordLifecycleService}.
 * It never duplicates the export logic and never lets a failure escape to the
 * scheduler: a failure is persisted as a {@code FAILED} record with a sanitized
 * {@code failureReason} (the administrator-visible alert surface).
 *
 * <p>Idempotency is database-backed and restart-safe: at most one
 * {@code SCHEDULED} record is created per local day, regardless of success or
 * failure, so the 60-second poll and application restarts cannot produce a
 * duplicate run. The {@link AtomicBoolean} additionally prevents two in-JVM
 * polls from overlapping. Multi-instance deduplication is intentionally not
 * provided (no distributed lock exists in this project).</p>
 */
@Service
@RequiredArgsConstructor
public class ExecuteScheduledBackupService implements ExecuteScheduledBackupUseCase {

    private static final String DESCRIPTION = "Automatic scheduled backup";

    private final BackupScheduleRepository scheduleRepository;
    private final BackupRecordRepository backupRecordRepository;
    private final BackupRecordLifecycleService lifecycleService;
    private final BackupSnapshotExportService snapshotExportService;
    private final BackupCodeGenerator backupCodeGenerator;
    private final BackupAuditLogWriter auditLogWriter;
    private final BackupFailureReason backupFailureReason;
    private final BackupScheduleProperties properties;
    private final ClockPort clockPort;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public void execute() {
        Instant now = clockPort.now();

        BackupSchedule schedule = scheduleRepository.find().orElse(null);
        if (schedule == null || !schedule.isEnabled()) {
            return;
        }
        if (now.isBefore(dueInstant(schedule, now))) {
            return;
        }
        if (alreadyRanToday(now)) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            run(now);
        } finally {
            running.set(false);
        }
    }

    private void run(Instant now) {
        UUID actorId = BackupSystemActor.SYSTEM_USER_ID;
        String backupCode = backupCodeGenerator.generate();
        BackupRecord record = lifecycleService.createInProgress(
                backupCode, BackupType.SCHEDULED, DESCRIPTION, actorId, now);

        try {
            BackupSnapshot snapshot = snapshotExportService.export(backupCode);
            record = lifecycleService.markSuccess(record.getId(), snapshot);
            auditLogWriter.write(actorId, ActionType.BACKUP, record.getId(), toDetail(record, null));
        } catch (RuntimeException ex) {
            String reason = backupFailureReason.sanitize(ex);
            record = lifecycleService.markFailed(record.getId(), reason);
            auditLogWriter.write(actorId, ActionType.BACKUP, record.getId(), toDetail(record, reason));
        }
    }

    private Instant dueInstant(BackupSchedule schedule, Instant now) {
        return now.atZone(properties.zoneId())
                .toLocalDate()
                .atTime(schedule.getBackupTime())
                .atZone(properties.zoneId())
                .toInstant();
    }

    private boolean alreadyRanToday(Instant now) {
        Instant todayStart = now.atZone(properties.zoneId())
                .toLocalDate()
                .atStartOfDay(properties.zoneId())
                .toInstant();
        return backupRecordRepository.findTopByBackupTypeOrderByCreatedAtDesc(BackupType.SCHEDULED)
                .map(latest -> !latest.getCreatedAt().isBefore(todayStart))
                .orElse(false);
    }

    private String toDetail(BackupRecord record, String failureReason) {
        String reasonJson = failureReason == null ? "null" : "\"" + failureReason.replace("\"", "'") + "\"";
        return "{\"backupCode\":\"" + record.getBackupCode() + "\","
                + "\"fileName\":\"" + (record.getFileName() == null ? "" : record.getFileName()) + "\","
                + "\"fileSize\":" + record.getFileSize() + ","
                + "\"backupType\":\"" + record.getBackupType() + "\","
                + "\"status\":\"" + record.getStatus() + "\","
                + "\"failureReason\":" + reasonJson + "}";
    }
}

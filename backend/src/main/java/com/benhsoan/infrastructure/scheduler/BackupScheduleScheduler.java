package com.benhsoan.infrastructure.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.benhsoan.port.inbound.backup.ExecuteScheduledBackupUseCase;

import lombok.RequiredArgsConstructor;

/**
 * Polls the automatic backup schedule. All business logic (enabled check,
 * due-time evaluation, idempotency, execution and failure recording) lives in
 * {@link ExecuteScheduledBackupUseCase}; this component only triggers it.
 */
@Component
@RequiredArgsConstructor
public class BackupScheduleScheduler {

    private final ExecuteScheduledBackupUseCase executeScheduledBackupUseCase;

    @Scheduled(fixedDelayString = "${app.backup.schedule.scan-interval-ms:60000}")
    public void checkAndRun() {
        executeScheduledBackupUseCase.execute();
    }
}

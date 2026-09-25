package com.benhsoan.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.benhsoan.port.inbound.backup.ExecuteScheduledBackupUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduledBackupJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledBackupJob.class);

    private final ExecuteScheduledBackupUseCase executeScheduledBackupUseCase;

    @Scheduled(cron = "${app.backup.schedule.scan-cron:0 * * * * *}")
    public void scanAndExecute() {
        try {
            executeScheduledBackupUseCase.executeIfDue();
        } catch (Exception ex) {
            log.error("Error executing scheduled backup job: {}", ex.getMessage(), ex);
        }
    }
}

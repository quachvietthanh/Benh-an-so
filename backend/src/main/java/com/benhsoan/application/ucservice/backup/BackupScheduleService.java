package com.benhsoan.application.ucservice.backup;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.backup.BackupScheduleConfiguration;
import com.benhsoan.port.inbound.backup.DismissBackupAlertUseCase;
import com.benhsoan.port.inbound.backup.GetBackupScheduleUseCase;
import com.benhsoan.port.inbound.backup.UpdateBackupScheduleUseCase;
import com.benhsoan.port.outbound.repository.backup.BackupScheduleRepositoryPort;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupScheduleService implements GetBackupScheduleUseCase, UpdateBackupScheduleUseCase, DismissBackupAlertUseCase {

    private final BackupScheduleRepositoryPort scheduleRepository;
    private final BackupAuthorizer authorizer;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final BackupAuditLogWriter auditLogWriter;

    @Override
    @Transactional(readOnly = true)
    public BackupScheduleConfiguration getSchedule() {
        authorizer.requireAdmin();
        return getOrCreateConfig();
    }

    @Override
    @Transactional
    public BackupScheduleConfiguration updateSchedule(boolean enabled, String dailyTime) {
        authorizer.requireAdmin();

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        BackupScheduleConfiguration config = getOrCreateConfig();
        config.updateSchedule(enabled, dailyTime, actorId, now);
        BackupScheduleConfiguration saved = scheduleRepository.save(config);

        String detail = """
                {"enabled":%s,"dailyTime":"%s","cronExpression":"%s"}
                """.formatted(saved.isEnabled(), saved.getDailyTime(), saved.getCronExpression()).trim();
        auditLogWriter.write(actorId, ActionType.UPDATE, saved.getId(), detail);

        return saved;
    }

    @Override
    @Transactional
    public BackupScheduleConfiguration dismissAlert() {
        authorizer.requireAdmin();

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        BackupScheduleConfiguration config = getOrCreateConfig();
        config.dismissAlert(actorId, now);
        BackupScheduleConfiguration saved = scheduleRepository.save(config);

        String detail = """
                {"alertActive":false,"action":"DISMISS_ALERT"}
                """.trim();
        auditLogWriter.write(actorId, ActionType.UPDATE, saved.getId(), detail);

        return saved;
    }

    private BackupScheduleConfiguration getOrCreateConfig() {
        return scheduleRepository.find()
                .orElseGet(() -> {
                    UUID actorId = currentUserPort.getCurrentUserId();
                    Instant now = clockPort.now();
                    return scheduleRepository.save(BackupScheduleConfiguration.createDefault(actorId, now));
                });
    }
}

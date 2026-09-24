package com.benhsoan.application.ucservice.backup;

import java.time.Instant;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.auditlog.AdminOperationAuditService;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.backup.BackupSchedule;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.backup.UpdateBackupScheduleCommand;
import com.benhsoan.port.dto.result.BackupScheduleResult;
import com.benhsoan.port.inbound.backup.UpdateBackupScheduleUseCase;
import com.benhsoan.port.outbound.repository.backup.BackupScheduleRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateBackupScheduleService implements UpdateBackupScheduleUseCase {

    private final BackupScheduleRepository scheduleRepository;
    private final BackupAuthorizer authorizer;
    private final AdminOperationAuditService adminOperationAuditService;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public BackupScheduleResult update(UpdateBackupScheduleCommand command) {
        authorizer.requireAdmin();

        if (command == null || command.backupTime() == null) {
            throw new ValidationException("Backup time is required.");
        }

        Instant now = clockPort.now();
        BackupSchedule schedule = scheduleRepository.find()
                .orElseGet(() -> BackupSchedule.createDefault(now));

        boolean beforeEnabled = schedule.isEnabled();
        LocalTime beforeTime = schedule.getBackupTime();

        schedule.update(command.enabled(), command.backupTime(), now);
        BackupSchedule saved = scheduleRepository.save(schedule);

        adminOperationAuditService.record(
                currentUserPort.getCurrentUserId(),
                ActionType.UPDATE,
                ResourceType.CONFIGURATION,
                null,
                AdminOperationAuditService.fields(
                        "enabled", beforeEnabled,
                        "backupTime", beforeTime != null ? beforeTime.toString() : null
                ),
                AdminOperationAuditService.fields(
                        "enabled", saved.isEnabled(),
                        "backupTime", saved.getBackupTime().toString()
                ),
                now
        );

        return new BackupScheduleResult(
                saved.isEnabled(),
                saved.getBackupTime(),
                saved.getUpdatedAt()
        );
    }
}

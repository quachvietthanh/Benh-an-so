package com.benhsoan.application.ucservice.backup;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.backup.BackupSchedule;
import com.benhsoan.port.dto.result.BackupScheduleResult;
import com.benhsoan.port.inbound.backup.GetBackupScheduleUseCase;
import com.benhsoan.port.outbound.repository.backup.BackupScheduleRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetBackupScheduleService implements GetBackupScheduleUseCase {

    private final BackupScheduleRepository scheduleRepository;
    private final BackupAuthorizer authorizer;
    private final ClockPort clockPort;

    @Override
    public BackupScheduleResult get() {
        authorizer.requireAdmin();

        BackupSchedule schedule = scheduleRepository.find()
                .orElseGet(() -> BackupSchedule.createDefault(clockPort.now()));

        return new BackupScheduleResult(
                schedule.isEnabled(),
                schedule.getBackupTime(),
                schedule.getUpdatedAt()
        );
    }
}

package com.benhsoan.port.outbound.repository.backup;

import java.util.Optional;

import com.benhsoan.domain.backup.BackupScheduleConfiguration;

public interface BackupScheduleRepositoryPort {

    Optional<BackupScheduleConfiguration> find();

    BackupScheduleConfiguration save(BackupScheduleConfiguration configuration);
}

package com.benhsoan.port.outbound.repository.backup;

import java.util.Optional;

import com.benhsoan.domain.backup.BackupSchedule;

public interface BackupScheduleRepository {

    Optional<BackupSchedule> find();

    BackupSchedule save(BackupSchedule schedule);
}

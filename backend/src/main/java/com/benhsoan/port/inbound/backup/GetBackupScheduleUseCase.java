package com.benhsoan.port.inbound.backup;

import com.benhsoan.domain.backup.BackupScheduleConfiguration;

public interface GetBackupScheduleUseCase {

    BackupScheduleConfiguration getSchedule();
}

package com.benhsoan.port.inbound.backup;

import com.benhsoan.domain.backup.BackupScheduleConfiguration;

public interface UpdateBackupScheduleUseCase {

    BackupScheduleConfiguration updateSchedule(boolean enabled, String dailyTime);
}

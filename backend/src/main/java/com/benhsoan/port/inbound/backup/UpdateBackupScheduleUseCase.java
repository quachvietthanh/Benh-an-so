package com.benhsoan.port.inbound.backup;

import com.benhsoan.port.dto.command.backup.UpdateBackupScheduleCommand;
import com.benhsoan.port.dto.result.BackupScheduleResult;

public interface UpdateBackupScheduleUseCase {

    BackupScheduleResult update(UpdateBackupScheduleCommand command);
}

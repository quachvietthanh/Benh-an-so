package com.benhsoan.port.inbound.backup;

import java.util.List;

import com.benhsoan.port.dto.result.BackupResult;

public interface ListBackupsUseCase {

    List<BackupResult> list();
}

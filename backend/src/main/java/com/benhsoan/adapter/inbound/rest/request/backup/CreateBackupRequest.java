package com.benhsoan.adapter.inbound.rest.request.backup;

import com.benhsoan.domain.backup.enums.BackupType;

import jakarta.validation.constraints.Size;

public record CreateBackupRequest(
        BackupType backupType,

        @Size(max = 255, message = "Description must not exceed 255 characters.")
        String description
) {
}

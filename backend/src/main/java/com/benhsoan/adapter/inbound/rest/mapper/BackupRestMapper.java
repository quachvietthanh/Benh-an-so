package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.backup.CreateBackupRequest;
import com.benhsoan.adapter.inbound.rest.response.backup.BackupResponse;
import com.benhsoan.port.dto.command.backup.CreateBackupCommand;
import com.benhsoan.port.dto.result.BackupResult;

@Component
public class BackupRestMapper {

    public BackupResponse toResponse(BackupResult result) {
        return new BackupResponse(
                result.id(),
                result.backupCode(),
                result.fileName(),
                result.fileSize(),
                result.status(),
                result.backupType(),
                result.description(),
                result.createdBy(),
                result.createdAt(),
                result.restoredAt(),
                result.restoredBy()
        );
    }

    public List<BackupResponse> toResponse(List<BackupResult> results) {
        return results.stream().map(this::toResponse).toList();
    }

    public CreateBackupCommand toCommand(CreateBackupRequest request) {
        return new CreateBackupCommand(request.backupType(), request.description());
    }
}

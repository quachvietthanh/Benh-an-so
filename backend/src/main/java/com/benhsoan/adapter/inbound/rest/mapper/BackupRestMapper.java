package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.backup.CreateBackupRequest;
import com.benhsoan.adapter.inbound.rest.request.backup.UpdateBackupScheduleRequest;
import com.benhsoan.adapter.inbound.rest.response.backup.BackupIntegrityResponse;
import com.benhsoan.adapter.inbound.rest.response.backup.BackupResponse;
import com.benhsoan.adapter.inbound.rest.response.backup.BackupScheduleResponse;
import com.benhsoan.port.dto.command.backup.CreateBackupCommand;
import com.benhsoan.port.dto.command.backup.UpdateBackupScheduleCommand;
import com.benhsoan.port.dto.result.BackupIntegrityResult;
import com.benhsoan.port.dto.result.BackupResult;
import com.benhsoan.port.dto.result.BackupScheduleResult;

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
                result.failureReason(),
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

    public UpdateBackupScheduleCommand toCommand(UpdateBackupScheduleRequest request) {
        return new UpdateBackupScheduleCommand(request.enabled(), request.backupTime());
    }

    public BackupScheduleResponse toResponse(BackupScheduleResult result) {
        return new BackupScheduleResponse(
                result.enabled(),
                result.backupTime(),
                result.updatedAt()
        );
    }

    public BackupIntegrityResponse toResponse(BackupIntegrityResult result) {
        return new BackupIntegrityResponse(
                result.backupId(),
                result.backupCode(),
                result.fileName(),
                result.backupCreatedAt(),
                result.valid(),
                result.reason(),
                result.tableCount(),
                result.rowCount(),
                result.verifiedAt()
        );
    }
}


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
                result.restoredBy(),
                result.failureReason()
        );
    }

    public List<BackupResponse> toResponse(List<BackupResult> results) {
        return results.stream().map(this::toResponse).toList();
    }

    public CreateBackupCommand toCommand(CreateBackupRequest request) {
        return new CreateBackupCommand(request.backupType(), request.description());
    }

    public com.benhsoan.adapter.inbound.rest.response.backup.BackupScheduleResponse toResponse(
            com.benhsoan.domain.backup.BackupScheduleConfiguration config
    ) {
        if (config == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.backup.BackupScheduleResponse(
                config.getId(),
                config.isEnabled(),
                config.getDailyTime(),
                config.getCronExpression(),
                config.getLastRunAt(),
                config.getLastStatus(),
                config.getLastFailureReason(),
                config.isAlertActive(),
                config.getLastVerifiedAt(),
                config.getLastVerificationStatus(),
                config.getUpdatedBy(),
                config.getUpdatedAt()
        );
    }

    public com.benhsoan.adapter.inbound.rest.response.backup.BackupVerificationResponse toResponse(
            com.benhsoan.domain.backup.BackupVerificationReport report
    ) {
        if (report == null) {
            return null;
        }
        return new com.benhsoan.adapter.inbound.rest.response.backup.BackupVerificationResponse(
                report.backupId(),
                report.backupCode(),
                report.fileName(),
                report.valid(),
                report.readable(),
                report.dataIntact(),
                report.tableCount(),
                report.rowCount(),
                report.schemaVersion(),
                report.verifiedAt(),
                report.message(),
                report.issues()
        );
    }
}

package com.benhsoan.adapter.inbound.rest.response.backup;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.backup.enums.BackupStatus;

public record BackupScheduleResponse(
        UUID id,
        boolean enabled,
        String dailyTime,
        String cronExpression,
        Instant lastRunAt,
        BackupStatus lastStatus,
        String lastFailureReason,
        boolean alertActive,
        Instant lastVerifiedAt,
        String lastVerificationStatus,
        UUID updatedBy,
        Instant updatedAt
) {
}

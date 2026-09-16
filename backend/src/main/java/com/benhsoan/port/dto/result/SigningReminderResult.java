package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record SigningReminderResult(
        UUID id,
        UUID medicalRecordId,
        UUID doctorId,
        String doctorFullName,
        UUID remindedBy,
        String remindedByName,
        Instant remindedAt,
        long overdueHours,
        String channel,
        String notes,
        String status
) {
}

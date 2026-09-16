package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.UUID;

public record SigningReminderResponse(
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

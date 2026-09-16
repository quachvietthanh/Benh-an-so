package com.benhsoan.port.dto.command.medicalrecord;

import java.util.UUID;

public record SendSigningReminderCommand(
        UUID medicalRecordId,
        String channel,
        String notes
) {

    public SendSigningReminderCommand(UUID medicalRecordId, String notes) {
        this(medicalRecordId, "SYSTEM", notes);
    }
}

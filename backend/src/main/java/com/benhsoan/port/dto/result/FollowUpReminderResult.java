package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;

public record FollowUpReminderResult(
        UUID id,
        UUID patientId,
        UUID visitId,
        UUID appointmentId,
        LocalDate followUpDate,
        Instant remindAt,
        ReminderType reminderType,
        ReminderStatus status,
        String notes,
        UUID createdBy,
        Instant createdAt
) {
}

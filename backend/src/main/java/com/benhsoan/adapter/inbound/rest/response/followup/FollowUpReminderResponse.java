package com.benhsoan.adapter.inbound.rest.response.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;

public record FollowUpReminderResponse(
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

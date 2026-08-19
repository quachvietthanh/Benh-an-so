package com.benhsoan.port.dto.command.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.followup.enums.ReminderType;

public record CreateFollowUpReminderCommand(
        UUID patientId,
        UUID visitId,
        UUID appointmentId,
        LocalDate followUpDate,
        Instant remindAt,
        ReminderType reminderType,
        String notes
) {
}

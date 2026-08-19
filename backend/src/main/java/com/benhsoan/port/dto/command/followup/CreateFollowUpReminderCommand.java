package com.benhsoan.port.dto.command.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.followup.enums.ReminderType;

import jakarta.validation.constraints.NotNull;

public record CreateFollowUpReminderCommand(
        UUID patientId,
        @NotNull(message = "visitId is required.")
        UUID visitId,
        UUID appointmentId,
        LocalDate followUpDate,
        Instant remindAt,
        ReminderType reminderType,
        String notes
) {
}

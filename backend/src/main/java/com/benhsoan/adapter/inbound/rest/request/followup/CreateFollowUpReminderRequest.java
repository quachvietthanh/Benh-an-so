package com.benhsoan.adapter.inbound.rest.request.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.followup.enums.ReminderType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFollowUpReminderRequest(
        @NotNull(message = "patientId is required.")
        UUID patientId,

        @NotNull(message = "visitId is required.")
        UUID visitId,

        UUID appointmentId,

        @NotNull(message = "followUpDate is required.")
        LocalDate followUpDate,

        @NotNull(message = "remindAt is required.")
        Instant remindAt,

        ReminderType reminderType,

        @Size(max = 500, message = "Notes must not exceed 500 characters.")
        String notes
) {
}

package com.benhsoan.adapter.inbound.rest.request.followup;

import com.benhsoan.domain.followup.enums.ReminderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateFollowUpReminderStatusRequest(
        @NotNull(message = "status is required.")
        ReminderStatus status
) {
}

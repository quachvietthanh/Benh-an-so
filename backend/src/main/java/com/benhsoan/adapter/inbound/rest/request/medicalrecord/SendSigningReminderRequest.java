package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import jakarta.validation.constraints.Size;

public record SendSigningReminderRequest(
        @Size(max = 500, message = "Notes must not exceed 500 characters.")
        String notes,

        @Size(max = 30, message = "Channel must not exceed 30 characters.")
        String channel
) {
}

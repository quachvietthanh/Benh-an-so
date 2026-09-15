package com.benhsoan.adapter.inbound.rest.request.queue;

import com.benhsoan.domain.visit.enums.VisitCloseOutcome;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CloseQueueItemRequest(
        @NotNull(message = "Outcome is required.") VisitCloseOutcome outcome,
        @NotBlank(message = "Close reason is required.")
        @Size(max = 500, message = "Close reason must not exceed 500 characters.")
        String reason
) {
}

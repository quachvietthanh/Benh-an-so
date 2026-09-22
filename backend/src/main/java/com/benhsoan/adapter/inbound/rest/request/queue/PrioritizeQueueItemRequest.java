package com.benhsoan.adapter.inbound.rest.request.queue;

import com.benhsoan.domain.queue.enums.QueuePriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrioritizeQueueItemRequest(
        @NotNull(message = "Priority level is required")
        QueuePriority priority,

        @NotBlank(message = "Priority reason is required")
        @Size(max = 500, message = "Priority reason cannot exceed 500 characters")
        String reason
) {
}

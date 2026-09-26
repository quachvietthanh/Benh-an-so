package com.benhsoan.adapter.inbound.rest.request.queue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkipQueueItemRequest(
        @NotBlank
        @Size(max = 500)
        String reason
) {
}

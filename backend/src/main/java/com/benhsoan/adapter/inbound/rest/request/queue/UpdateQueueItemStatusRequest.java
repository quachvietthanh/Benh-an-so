package com.benhsoan.adapter.inbound.rest.request.queue;

import com.benhsoan.domain.queue.enums.QueueItemStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateQueueItemStatusRequest(
        @NotNull QueueItemStatus targetStatus,
        @Size(max = 500) String cancelReason
) {
}

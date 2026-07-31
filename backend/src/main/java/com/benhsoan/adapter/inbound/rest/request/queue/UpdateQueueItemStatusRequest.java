package com.benhsoan.adapter.inbound.rest.request.queue;

import com.benhsoan.domain.queue.enums.QueueItemStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateQueueItemStatusRequest(
        @NotNull QueueItemStatus targetStatus,
        String cancelReason
) {
}

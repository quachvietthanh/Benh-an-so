package com.benhsoan.adapter.inbound.rest.response.queue;

import java.time.Instant;
import java.util.UUID;

public record QueueHistoryResponse(
        UUID id,
        UUID queueItemId,
        UUID operatorId,
        String operatorName,
        String action,
        String status,
        int callCount,
        String reason,
        Instant timestamp
) {
}

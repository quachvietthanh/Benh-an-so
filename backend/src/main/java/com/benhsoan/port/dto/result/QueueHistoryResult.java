package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record QueueHistoryResult(
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

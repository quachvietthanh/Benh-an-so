package com.benhsoan.port.dto.command.queue;

import java.util.UUID;

import com.benhsoan.domain.queue.enums.QueuePriority;

public record PrioritizeQueueItemCommand(
        UUID queueItemId,
        QueuePriority priority,
        String reason
) {
}

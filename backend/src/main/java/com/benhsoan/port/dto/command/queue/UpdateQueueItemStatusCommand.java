package com.benhsoan.port.dto.command.queue;

import java.util.UUID;

import com.benhsoan.domain.queue.enums.QueueItemStatus;

public record UpdateQueueItemStatusCommand(UUID queueItemId, QueueItemStatus targetStatus, String cancelReason) {
}

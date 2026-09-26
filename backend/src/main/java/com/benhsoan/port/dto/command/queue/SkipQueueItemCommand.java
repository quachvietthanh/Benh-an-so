package com.benhsoan.port.dto.command.queue;

import java.util.UUID;

public record SkipQueueItemCommand(UUID queueItemId, String reason) {
}

package com.benhsoan.port.inbound.queue;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.QueueHistoryResult;

public interface GetQueueHistoryUseCase {
    List<QueueHistoryResult> getHistory(UUID queueItemId);
}

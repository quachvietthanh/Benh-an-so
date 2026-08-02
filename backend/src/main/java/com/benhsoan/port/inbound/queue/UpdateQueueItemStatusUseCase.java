package com.benhsoan.port.inbound.queue;

import com.benhsoan.port.dto.command.queue.UpdateQueueItemStatusCommand;
import com.benhsoan.port.dto.result.QueueItemResult;

public interface UpdateQueueItemStatusUseCase {

    QueueItemResult updateStatus(UpdateQueueItemStatusCommand command);
}

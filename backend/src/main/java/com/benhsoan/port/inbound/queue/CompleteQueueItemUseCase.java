package com.benhsoan.port.inbound.queue;

import com.benhsoan.port.dto.command.queue.CompleteQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;

public interface CompleteQueueItemUseCase {

    QueueItemResult complete(CompleteQueueItemCommand command);
}

package com.benhsoan.port.inbound.queue;

import com.benhsoan.port.dto.command.queue.SkipQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;

public interface SkipQueueItemUseCase {

    QueueItemResult skip(SkipQueueItemCommand command);
}

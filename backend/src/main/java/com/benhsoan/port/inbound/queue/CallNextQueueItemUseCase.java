package com.benhsoan.port.inbound.queue;

import com.benhsoan.port.dto.command.queue.CallNextQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;

public interface CallNextQueueItemUseCase {

    QueueItemResult callNext(CallNextQueueItemCommand command);
}

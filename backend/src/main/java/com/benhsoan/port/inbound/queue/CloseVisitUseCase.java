package com.benhsoan.port.inbound.queue;

import com.benhsoan.port.dto.command.queue.CloseVisitCommand;
import com.benhsoan.port.dto.result.QueueItemResult;

public interface CloseVisitUseCase {

    QueueItemResult close(CloseVisitCommand command);
}

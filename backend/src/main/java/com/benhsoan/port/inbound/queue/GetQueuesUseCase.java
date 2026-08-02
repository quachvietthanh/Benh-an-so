package com.benhsoan.port.inbound.queue;

import java.util.List;

import com.benhsoan.port.dto.command.queue.GetQueuesQuery;
import com.benhsoan.port.dto.result.QueueItemResult;

public interface GetQueuesUseCase {

    List<QueueItemResult> getQueues(GetQueuesQuery query);
}

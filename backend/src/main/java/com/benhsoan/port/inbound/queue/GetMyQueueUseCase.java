package com.benhsoan.port.inbound.queue;

import java.util.List;

import com.benhsoan.port.dto.command.queue.GetMyQueueQuery;
import com.benhsoan.port.dto.result.QueueItemResult;

public interface GetMyQueueUseCase {

    List<QueueItemResult> getMyQueue(GetMyQueueQuery query);
}

package com.benhsoan.port.inbound.queue;

import com.benhsoan.port.dto.command.queue.GetQueueDisplayQuery;
import com.benhsoan.port.dto.result.WaitingRoomBoardResult;

public interface GetQueueDisplayUseCase {
    WaitingRoomBoardResult getWaitingRoomDisplay(GetQueueDisplayQuery query);
}

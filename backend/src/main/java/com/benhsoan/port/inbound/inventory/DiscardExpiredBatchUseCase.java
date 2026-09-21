package com.benhsoan.port.inbound.inventory;

import com.benhsoan.port.dto.command.inventory.DiscardExpiredBatchCommand;
import com.benhsoan.port.dto.result.DiscardBatchResult;

public interface DiscardExpiredBatchUseCase {

    DiscardBatchResult discardExpired(DiscardExpiredBatchCommand command);
}

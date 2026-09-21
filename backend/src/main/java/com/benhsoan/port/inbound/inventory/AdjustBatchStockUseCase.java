package com.benhsoan.port.inbound.inventory;

import com.benhsoan.port.dto.command.inventory.AdjustBatchStockCommand;
import com.benhsoan.port.dto.result.BatchAdjustmentResult;

public interface AdjustBatchStockUseCase {

    BatchAdjustmentResult adjustStock(AdjustBatchStockCommand command);
}

package com.benhsoan.port.dto.command.inventory;

import java.util.UUID;

public record AdjustBatchStockCommand(
        UUID batchId,
        int actualQuantity,
        String reason
) {
}

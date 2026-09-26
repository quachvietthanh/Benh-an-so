package com.benhsoan.port.dto.command.inventory;

import java.util.List;

public record ReceiveStockCommand(
        String note,
        List<ReceiveStockItemCommand> items
) {
}

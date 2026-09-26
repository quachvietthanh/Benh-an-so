package com.benhsoan.port.inbound.inventory;

import com.benhsoan.port.dto.command.inventory.ReceiveStockCommand;
import com.benhsoan.port.dto.result.InventoryReceiptResult;

public interface ReceiveStockUseCase {

    InventoryReceiptResult receiveStock(ReceiveStockCommand command);
}

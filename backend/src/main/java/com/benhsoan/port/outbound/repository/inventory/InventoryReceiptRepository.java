package com.benhsoan.port.outbound.repository.inventory;

import com.benhsoan.domain.inventory.InventoryReceipt;

public interface InventoryReceiptRepository {

    InventoryReceipt save(InventoryReceipt receipt);
}

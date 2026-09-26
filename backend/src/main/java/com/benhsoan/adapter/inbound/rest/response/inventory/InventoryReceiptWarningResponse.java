package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.util.UUID;

public record InventoryReceiptWarningResponse(
        String code,
        UUID medicineId,
        String batchNumber,
        String message
) {
}

package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.util.UUID;

public record InventoryInOutStockItemResponse(
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String unit,
        int openingStock,
        int importQuantity,
        int dispensedQuantity,
        int returnedQuantity,
        int adjustedQuantity,
        int closingStock
) {
}

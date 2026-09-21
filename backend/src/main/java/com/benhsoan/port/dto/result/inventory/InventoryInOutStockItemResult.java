package com.benhsoan.port.dto.result.inventory;

import java.util.UUID;

public record InventoryInOutStockItemResult(
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

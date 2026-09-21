package com.benhsoan.port.dto.result.inventory;

public record InventoryInOutStockSummaryResult(
        int totalMedicines,
        int totalOpeningStock,
        int totalImportQuantity,
        int totalDispensedQuantity,
        int totalReturnedQuantity,
        int totalAdjustedQuantity,
        int totalClosingStock
) {
}

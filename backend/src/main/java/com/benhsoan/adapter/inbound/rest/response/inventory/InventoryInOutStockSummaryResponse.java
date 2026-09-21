package com.benhsoan.adapter.inbound.rest.response.inventory;

public record InventoryInOutStockSummaryResponse(
        int totalMedicines,
        int totalOpeningStock,
        int totalImportQuantity,
        int totalDispensedQuantity,
        int totalReturnedQuantity,
        int totalAdjustedQuantity,
        int totalClosingStock
) {
}

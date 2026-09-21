package com.benhsoan.port.dto.result;

import java.util.UUID;

public record InventoryStockReportItemResult(
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String unit,
        int openingQuantity,
        int receivedQuantity,
        int dispensedQuantity,
        int returnedQuantity,
        int adjustedQuantity,
        int closingQuantity
) {
}

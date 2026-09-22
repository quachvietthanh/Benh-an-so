package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.util.UUID;

public record InventoryStockReportItemResponse(
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

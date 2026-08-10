package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.time.LocalDate;
import java.util.UUID;

public record InventoryStockResponse(
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String activeIngredient,
        String strength,
        String unit,
        boolean active,
        int stockQuantity,
        int eligibleStockQuantity,
        int activeBatchCount,
        LocalDate nearestExpiryDate
) {
}

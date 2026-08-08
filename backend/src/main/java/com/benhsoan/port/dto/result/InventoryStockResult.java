package com.benhsoan.port.dto.result;

import java.time.LocalDate;
import java.util.UUID;

public record InventoryStockResult(
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

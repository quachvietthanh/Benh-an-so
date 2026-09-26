package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.util.UUID;

public record LowStockMedicineResponse(
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String unit,
        int stockQuantity,
        int eligibleStockQuantity,
        int minStockThreshold,
        int shortageQuantity
) {
}

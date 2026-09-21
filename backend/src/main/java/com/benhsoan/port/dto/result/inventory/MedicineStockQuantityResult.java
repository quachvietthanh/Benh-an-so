package com.benhsoan.port.dto.result.inventory;

import java.util.UUID;

public record MedicineStockQuantityResult(
        UUID medicineId,
        long totalQuantity
) {
}

package com.benhsoan.port.dto.result.inventory;

import java.util.UUID;

import com.benhsoan.domain.inventory.enums.StockMovementType;

public record MedicineMovementSummaryResult(
        UUID medicineId,
        StockMovementType movementType,
        long totalQuantityChange
) {
}

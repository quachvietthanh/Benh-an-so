package com.benhsoan.port.dto.command.inventory;

import java.util.UUID;

public record CreateProcurementPlanItemCommand(
        UUID medicineId,
        int currentStock,
        int minStockThreshold,
        int previousPeriodConsumption,
        int suggestedQuantity,
        int proposedQuantity,
        String note
) {
}

package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record ProcurementPlanItemResult(
        UUID id,
        UUID planId,
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String unit,
        int currentStock,
        int minStockThreshold,
        int previousPeriodConsumption,
        int suggestedQuantity,
        int proposedQuantity,
        int approvedQuantity,
        String note,
        Instant createdAt
) {
}

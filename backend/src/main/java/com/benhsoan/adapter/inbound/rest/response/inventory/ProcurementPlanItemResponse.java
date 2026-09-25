package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.time.Instant;
import java.util.UUID;

public record ProcurementPlanItemResponse(
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

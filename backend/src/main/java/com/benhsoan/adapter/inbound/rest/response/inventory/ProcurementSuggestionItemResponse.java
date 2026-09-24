package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.util.UUID;

public record ProcurementSuggestionItemResponse(
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String unit,
        int currentStock,
        int eligibleStock,
        int minStockThreshold,
        int previousPeriodConsumption,
        int suggestedQuantity
) {
}

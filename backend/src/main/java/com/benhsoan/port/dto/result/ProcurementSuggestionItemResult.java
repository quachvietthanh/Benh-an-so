package com.benhsoan.port.dto.result;

import java.util.UUID;

public record ProcurementSuggestionItemResult(
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

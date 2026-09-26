package com.benhsoan.port.dto.result;

import java.util.UUID;

/**
 * Per-item dispensing summary: prescribed vs. actually dispensed vs. remaining.
 */
public record DispenseItemSummaryResult(
        UUID prescriptionItemId,
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String unit,
        int prescribedQuantity,
        int dispensedQuantity,
        int remainingQuantity
) {
}
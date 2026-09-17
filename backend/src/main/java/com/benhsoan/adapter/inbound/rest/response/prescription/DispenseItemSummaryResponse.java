package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.UUID;

public record DispenseItemSummaryResponse(
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
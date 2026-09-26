package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.time.LocalDate;
import java.util.UUID;

public record DispenseAllocationResponse(
        UUID dispenseItemId,
        UUID prescriptionItemId,
        UUID medicineId,
        String medicineCode,
        String medicineName,
        UUID batchId,
        String batchNumber,
        LocalDate expiryDate,
        int dispensedQuantity,
        int batchQuantityRemaining
) {
}

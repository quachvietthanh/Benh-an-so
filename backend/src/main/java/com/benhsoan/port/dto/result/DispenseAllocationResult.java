package com.benhsoan.port.dto.result;

import java.time.LocalDate;
import java.util.UUID;

public record DispenseAllocationResult(
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

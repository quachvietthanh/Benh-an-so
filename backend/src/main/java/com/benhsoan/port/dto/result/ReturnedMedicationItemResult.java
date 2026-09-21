package com.benhsoan.port.dto.result;

import java.util.UUID;

public record ReturnedMedicationItemResult(
        UUID returnId,
        UUID dispenseItemId,
        UUID prescriptionItemId,
        UUID medicineId,
        String medicineName,
        UUID medicineBatchId,
        String batchNumber,
        int returnedQuantity,
        int remainingReturnableQuantity
) {
}

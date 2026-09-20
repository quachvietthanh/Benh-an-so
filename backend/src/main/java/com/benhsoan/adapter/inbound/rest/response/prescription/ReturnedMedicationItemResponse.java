package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.UUID;

public record ReturnedMedicationItemResponse(
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

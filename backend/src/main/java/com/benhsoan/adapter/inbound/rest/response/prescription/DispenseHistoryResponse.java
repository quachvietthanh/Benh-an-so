package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.time.Instant;
import java.util.UUID;

public record DispenseHistoryResponse(
        UUID id,
        UUID prescriptionId,
        UUID prescriptionItemId,
        UUID medicineId,
        UUID medicineBatchId,
        int dispensedQuantity,
        UUID dispensedBy,
        Instant dispensedAt,
        String medicineName,
        String batchNumber,
        String dispenserName
) {
}
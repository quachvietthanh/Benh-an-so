package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record PrescriptionDispenseHistoryResult(
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
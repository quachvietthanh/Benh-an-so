package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.BatchStatus;

public record InventoryBatchResult(
        UUID batchId,
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String batchNumber,
        LocalDate expiryDate,
        int quantity,
        BatchStatus status,
        boolean eligibleForDispense,
        Instant createdAt,
        Instant updatedAt
) {
}

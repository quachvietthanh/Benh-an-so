package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.BatchStatus;

public record InventoryBatchResponse(
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

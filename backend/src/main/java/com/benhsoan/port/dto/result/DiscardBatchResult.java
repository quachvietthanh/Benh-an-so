package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.BatchStatus;

public record DiscardBatchResult(
        UUID batchId,
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String batchNumber,
        LocalDate expiryDate,
        int discardedQuantity,
        BatchStatus status,
        String reason,
        UUID performedBy,
        Instant performedAt
) {
}

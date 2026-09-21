package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.BatchStatus;

public record BatchAdjustmentResult(
        UUID batchId,
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String batchNumber,
        LocalDate expiryDate,
        int quantityBefore,
        int quantityAfter,
        int quantityChange,
        BatchStatus status,
        String reason,
        UUID performedBy,
        Instant performedAt
) {
}

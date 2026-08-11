package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.inventory.enums.InventoryExpiryAlertStatus;

public record InventoryExpiryAlertResult(
        UUID batchId,
        UUID medicineId,
        String medicineCode,
        String medicineName,
        String batchNumber,
        LocalDate expiryDate,
        int quantity,
        BatchStatus batchStatus,
        long daysToExpiry,
        InventoryExpiryAlertStatus alertStatus,
        Instant createdAt,
        Instant updatedAt
) {
}

package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.inventory.enums.InventoryExpiryAlertStatus;

public record InventoryExpiryAlertResponse(
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

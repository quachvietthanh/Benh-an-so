package com.benhsoan.port.dto.query.inventory;

import java.util.UUID;

import com.benhsoan.domain.inventory.enums.InventoryExpiryAlertStatus;

public record ListInventoryExpiryAlertsQuery(
        UUID medicineId,
        InventoryExpiryAlertStatus status
) {
}

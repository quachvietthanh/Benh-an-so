package com.benhsoan.port.dto.query.inventory;

import java.util.UUID;

import com.benhsoan.domain.inventory.enums.BatchStatus;

public record ListInventoryBatchesQuery(
        UUID medicineId,
        BatchStatus status,
        Boolean eligibleForDispense
) {
}

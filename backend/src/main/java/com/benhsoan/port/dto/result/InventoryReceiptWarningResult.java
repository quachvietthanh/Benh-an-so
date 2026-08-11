package com.benhsoan.port.dto.result;

import java.util.UUID;

public record InventoryReceiptWarningResult(
        String code,
        UUID medicineId,
        String batchNumber,
        String message
) {
}

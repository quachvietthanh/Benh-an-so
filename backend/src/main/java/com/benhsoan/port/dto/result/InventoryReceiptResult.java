package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryReceiptResult(
        UUID id,
        UUID receivedBy,
        Instant receivedAt,
        String note,
        Instant createdAt,
        List<InventoryReceiptItemResult> items,
        List<InventoryReceiptWarningResult> warnings
) {
}

package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryReceiptResponse(
        UUID id,
        UUID receivedBy,
        Instant receivedAt,
        String note,
        Instant createdAt,
        List<ReceiptItemResponse> items
) {
}

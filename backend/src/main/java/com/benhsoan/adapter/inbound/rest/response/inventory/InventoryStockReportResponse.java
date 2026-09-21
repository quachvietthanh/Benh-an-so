package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InventoryStockReportResponse(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        boolean hasTransactions,
        List<InventoryStockReportItemResponse> items
) {
}

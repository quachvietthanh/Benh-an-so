package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InventoryInOutStockReportResponse(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        boolean hasTransactions,
        List<InventoryInOutStockItemResponse> items,
        InventoryInOutStockSummaryResponse summary
) {
}

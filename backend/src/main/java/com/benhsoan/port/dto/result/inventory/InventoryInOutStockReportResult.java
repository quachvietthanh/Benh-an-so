package com.benhsoan.port.dto.result.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InventoryInOutStockReportResult(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        boolean hasTransactions,
        List<InventoryInOutStockItemResult> items,
        InventoryInOutStockSummaryResult summary
) {
}

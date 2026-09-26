package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InventoryStockReportResult(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        boolean hasTransactions,
        List<InventoryStockReportItemResult> items
) {
}

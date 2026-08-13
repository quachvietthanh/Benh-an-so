package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;

public record OperationalDashboardResult(
        VisitSummary visitSummary,
        RevenueSummary revenueSummary,
        InventoryAlertSummary inventoryAlertSummary,
        Instant asOf
) {

    public record VisitSummary(
            int total,
            int waiting,
            int inProgress,
            int completed,
            int cancelled
    ) {
    }

    public record RevenueSummary(
            BigDecimal totalRevenueToday
    ) {
    }

    public record InventoryAlertSummary(
            int lowStockCount,
            int expiryAlertCount
    ) {
    }
}

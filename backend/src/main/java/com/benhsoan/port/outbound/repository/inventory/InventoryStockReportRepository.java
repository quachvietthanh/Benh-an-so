package com.benhsoan.port.outbound.repository.inventory;

import java.time.Instant;
import java.util.List;

/**
 * Read-side port for the periodic stock in/out report (NCL-06-CN-013).
 *
 * <p>The implementation aggregates the authoritative inventory movement sources
 * (inventory receipt items and stock movements) into per-medicine summaries
 * using a bounded number of set-based queries (no N+1).</p>
 */
public interface InventoryStockReportRepository {

    List<InventoryStockMovementSummary> summarizeMovements(
            Instant fromInclusive,
            Instant toExclusive
    );
}

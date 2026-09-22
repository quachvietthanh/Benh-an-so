package com.benhsoan.port.outbound.repository.inventory;

import java.util.UUID;

/**
 * Aggregated stock movements for a single medicine over a reporting window.
 *
 * <p>Opening quantity is the net stock immediately before the window start.
 * The in-window movement categories follow the report formula
 * {@code closing = opening + received - dispensed + returned + adjusted}.</p>
 */
public record InventoryStockMovementSummary(
        UUID medicineId,
        int openingQuantity,
        int receivedQuantity,
        int dispensedQuantity,
        int returnedQuantity,
        int adjustedQuantity
) {
}

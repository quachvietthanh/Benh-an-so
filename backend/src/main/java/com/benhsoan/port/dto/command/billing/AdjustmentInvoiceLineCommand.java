package com.benhsoan.port.dto.command.billing;

import java.math.BigDecimal;
import java.util.UUID;

public record AdjustmentInvoiceLineCommand(
        String itemName,
        UUID referenceId,
        int quantity,
        BigDecimal unitPrice
) {
}

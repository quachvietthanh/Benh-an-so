package com.benhsoan.port.dto.command.billing;

import java.util.List;
import java.util.UUID;

public record AdjustInvoiceCommand(
        UUID originalInvoiceId,
        String adjustmentReason,
        List<AdjustmentInvoiceLineCommand> lines
) {
}

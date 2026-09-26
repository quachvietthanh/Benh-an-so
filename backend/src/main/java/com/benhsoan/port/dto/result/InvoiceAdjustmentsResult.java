package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InvoiceAdjustmentsResult(
        UUID originalInvoiceId,
        BigDecimal originalAmount,
        BigDecimal finalAmount,
        List<InvoiceResult> adjustments
) {
}

package com.benhsoan.adapter.inbound.rest.response.billing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InvoiceAdjustmentsResponse(
        UUID originalInvoiceId,
        BigDecimal originalAmount,
        BigDecimal finalAmount,
        List<InvoiceResponse> adjustments
) {
}

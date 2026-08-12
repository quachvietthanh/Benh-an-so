package com.benhsoan.adapter.inbound.rest.response.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.InvoiceType;

public record InvoiceResponse(
        UUID id,
        String invoiceCode,
        UUID visitId,
        UUID paymentId,
        InvoiceType type,
        UUID originalInvoiceId,
        String adjustmentReason,
        BigDecimal totalAmount,
        UUID createdBy,
        Instant createdAt,
        List<InvoiceLineResponse> lines
) {
}

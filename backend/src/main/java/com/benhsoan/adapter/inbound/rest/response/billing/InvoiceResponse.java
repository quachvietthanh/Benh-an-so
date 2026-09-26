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
        int reprintCount,
        Instant lastReprintedAt,
        List<InvoiceLineResponse> lines,
        PaymentDetailResponse payment
) {
    public InvoiceResponse(
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
            int reprintCount,
            Instant lastReprintedAt,
            List<InvoiceLineResponse> lines
    ) {
        this(
                id,
                invoiceCode,
                visitId,
                paymentId,
                type,
                originalInvoiceId,
                adjustmentReason,
                totalAmount,
                createdBy,
                createdAt,
                reprintCount,
                lastReprintedAt,
                lines,
                null
        );
    }
}

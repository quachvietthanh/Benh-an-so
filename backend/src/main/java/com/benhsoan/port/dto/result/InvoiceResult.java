package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.InvoiceType;

public record InvoiceResult(
        UUID id,
        String invoiceCode,
        UUID visitId,
        UUID paymentId,
        InvoiceType type,
        UUID originalInvoiceId,
        String adjustmentReason,
        BigDecimal discountAmount,
        UUID discountRequestId,
        BigDecimal totalAmount,
        UUID createdBy,
        Instant createdAt,
        int reprintCount,
        Instant lastReprintedAt,
        List<InvoiceLineResult> lines
) {
    public InvoiceResult(
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
            List<InvoiceLineResult> lines
    ) {
        this(
                id,
                invoiceCode,
                visitId,
                paymentId,
                type,
                originalInvoiceId,
                adjustmentReason,
                BigDecimal.ZERO,
                null,
                totalAmount,
                createdBy,
                createdAt,
                reprintCount,
                lastReprintedAt,
                lines
        );
    }
}

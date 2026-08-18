package com.benhsoan.adapter.inbound.rest.response.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentStatus;

public record RefundPaymentResponse(
        UUID paymentId,
        UUID visitId,
        PaymentStatus status,
        BigDecimal amountRefunded,
        String refundReason,
        UUID refundedBy,
        Instant refundedAt,
        InvoiceResponse adjustmentInvoice
) {
}

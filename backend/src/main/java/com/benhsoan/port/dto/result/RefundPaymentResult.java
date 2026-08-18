package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentStatus;

public record RefundPaymentResult(
        UUID paymentId,
        UUID visitId,
        PaymentStatus status,
        BigDecimal amountRefunded,
        String refundReason,
        UUID refundedBy,
        Instant refundedAt,
        InvoiceResult adjustmentInvoice
) {
}

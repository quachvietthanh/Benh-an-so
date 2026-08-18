package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;

public record PaymentResult(
        UUID id,
        UUID visitId,
        BigDecimal examFee,
        BigDecimal medicineFee,
        BigDecimal serviceFee,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        UUID collectedBy,
        Instant paidAt,
        Instant createdAt
) {
}

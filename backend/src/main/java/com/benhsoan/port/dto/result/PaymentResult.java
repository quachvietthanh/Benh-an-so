package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
        Instant createdAt,
        List<PaymentMethodItemResult> paymentMethods
) {
    public PaymentResult(
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
        this(
                id,
                visitId,
                examFee,
                medicineFee,
                serviceFee,
                totalAmount,
                amountPaid,
                paymentMethod,
                status,
                collectedBy,
                paidAt,
                createdAt,
                List.of()
        );
    }
}

package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.domain.billing.enums.PaymentStatus;

public record PaymentDetailResult(
        UUID id,
        UUID visitId,
        PaymentStatus status,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        UUID collectedBy,
        String collectorName,
        Instant paidAt,
        Instant createdAt,
        List<PaymentMethodItemResult> paymentMethods
) {
}

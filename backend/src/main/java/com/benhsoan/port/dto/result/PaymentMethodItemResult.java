package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentMethod;

public record PaymentMethodItemResult(
        UUID id,
        UUID paymentId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String referenceNumber,
        Instant createdAt
) {
}

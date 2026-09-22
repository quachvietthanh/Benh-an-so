package com.benhsoan.adapter.inbound.rest.response.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentMethod;

public record PaymentMethodItemResponse(
        UUID id,
        UUID paymentId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String referenceNumber,
        Instant createdAt
) {
}

package com.benhsoan.adapter.inbound.rest.response.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentQuoteResponse(
        UUID visitId,
        BigDecimal examFee,
        BigDecimal medicineFee,
        BigDecimal serviceFee,
        BigDecimal totalAmount,
        List<PaymentServiceFeeQuoteResponse> serviceFees,
        Instant calculatedAt
) {
}

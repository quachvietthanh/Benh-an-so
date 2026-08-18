package com.benhsoan.adapter.inbound.rest.response.billing;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentServiceFeeQuoteResponse(
        UUID clinicalOrderItemId,
        String serviceName,
        BigDecimal amount
) {
}

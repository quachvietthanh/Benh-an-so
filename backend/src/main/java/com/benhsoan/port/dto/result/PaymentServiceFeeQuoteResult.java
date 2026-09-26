package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentServiceFeeQuoteResult(
        UUID clinicalOrderItemId,
        String serviceName,
        BigDecimal amount
) {
}

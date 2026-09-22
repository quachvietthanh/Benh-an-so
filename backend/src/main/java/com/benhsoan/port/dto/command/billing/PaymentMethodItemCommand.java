package com.benhsoan.port.dto.command.billing;

import java.math.BigDecimal;

import com.benhsoan.domain.billing.enums.PaymentMethod;

public record PaymentMethodItemCommand(
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String referenceNumber
) {
}

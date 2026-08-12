package com.benhsoan.port.dto.command.billing;

import java.math.BigDecimal;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentMethod;

import lombok.Builder;

@Builder
public record RecordPaymentCommand(
        UUID visitId,
        BigDecimal examFee,
        BigDecimal medicineFee,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod
) {
}

package com.benhsoan.adapter.inbound.rest.request.billing;

import java.math.BigDecimal;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RecordPaymentRequest(
        @NotNull UUID visitId,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal examFee,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal medicineFee,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal amountPaid,
        @NotNull PaymentMethod paymentMethod
) {
}

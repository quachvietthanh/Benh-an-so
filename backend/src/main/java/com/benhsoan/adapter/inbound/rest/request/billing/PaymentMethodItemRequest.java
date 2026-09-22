package com.benhsoan.adapter.inbound.rest.request.billing;

import java.math.BigDecimal;

import com.benhsoan.domain.billing.enums.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentMethodItemRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
        @Size(max = 100, message = "Mã tham chiếu giao dịch không được vượt quá 100 ký tự.")
        String referenceNumber
) {
}

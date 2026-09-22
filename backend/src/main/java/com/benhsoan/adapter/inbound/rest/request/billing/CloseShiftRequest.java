package com.benhsoan.adapter.inbound.rest.request.billing;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CloseShiftRequest(
        @NotNull(message = "Số tiền mặt thực tế không được để trống.")
        @DecimalMin(value = "0.0", inclusive = true, message = "Số tiền mặt thực tế không được âm.")
        BigDecimal actualCashAmount,

        String notes
) {
}

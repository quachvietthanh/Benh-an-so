package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CheckMaxDailyDoseItemRequest(
        @NotNull
        UUID medicineId,

        @Positive
        BigDecimal singleDoseQuantity,

        @Positive
        Integer frequency
) {
}

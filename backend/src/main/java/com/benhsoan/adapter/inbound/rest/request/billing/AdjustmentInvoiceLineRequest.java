package com.benhsoan.adapter.inbound.rest.request.billing;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdjustmentInvoiceLineRequest(
        @NotBlank String itemName,
        UUID referenceId,
        @Positive int quantity,
        @NotNull BigDecimal unitPrice
) {
}

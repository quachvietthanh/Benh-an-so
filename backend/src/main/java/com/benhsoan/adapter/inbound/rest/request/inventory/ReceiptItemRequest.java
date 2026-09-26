package com.benhsoan.adapter.inbound.rest.request.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReceiptItemRequest(
        @NotNull(message = "Medicine id is required.")
        UUID medicineId,

        @NotBlank(message = "Batch number is required.")
        String batchNumber,

        @NotNull(message = "Expiry date is required.")
        @Future(message = "Expiry date must be in the future.")
        LocalDate expiryDate,

        @Min(value = 1, message = "Quantity must be greater than 0.")
        int quantity,

        @NotNull(message = "Import price is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "Import price must be non-negative.")
        BigDecimal importPrice
) {
}

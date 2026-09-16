package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DispenseItemRequest(
        @NotNull(message = "prescriptionItemId is required")
        UUID prescriptionItemId,

        @Min(value = 1, message = "quantity must be greater than zero")
        int quantity
) {
}
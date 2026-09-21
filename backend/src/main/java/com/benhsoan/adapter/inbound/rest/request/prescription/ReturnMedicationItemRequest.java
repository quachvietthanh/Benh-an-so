package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReturnMedicationItemRequest(
        @NotNull(message = "dispenseItemId is required") UUID dispenseItemId,
        @Min(value = 1, message = "quantity must be greater than zero") int quantity
) {
}

package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record ReturnMedicationRequest(
        @NotBlank(message = "reason is required") String reason,
        @NotEmpty(message = "items is required") @Valid List<ReturnMedicationItemRequest> items
) {
}

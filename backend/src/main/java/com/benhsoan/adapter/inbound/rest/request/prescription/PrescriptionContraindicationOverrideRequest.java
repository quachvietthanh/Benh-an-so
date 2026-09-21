package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PrescriptionContraindicationOverrideRequest(
        @NotNull(message = "ruleId is required") UUID ruleId,
        @NotNull(message = "medicineId is required") UUID medicineId,
        @NotBlank(message = "overrideReason is required") String overrideReason
) {
}

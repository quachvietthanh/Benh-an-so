package com.benhsoan.adapter.inbound.rest.request.prescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionMaxDailyDoseOverrideRequest(
        @NotBlank
        @Size(max = 255)
        String activeIngredient,

        @NotBlank
        @Size(max = 500)
        String overrideReason
) {
}

package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PrescriptionAllergyOverrideRequest(

        @NotNull
        UUID allergyId,

        @NotNull
        UUID medicineId,

        @NotBlank
        String overrideReason

) {
}

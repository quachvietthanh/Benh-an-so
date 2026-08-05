package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PrescriptionInteractionOverrideRequest(

        @NotNull
        UUID drugInteractionId,

        @NotBlank
        String overrideReason

) {
}

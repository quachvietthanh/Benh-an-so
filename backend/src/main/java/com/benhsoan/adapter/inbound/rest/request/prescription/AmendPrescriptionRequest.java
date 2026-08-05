package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record AmendPrescriptionRequest(

        String note,

        @NotBlank
        String changeReason,

        @NotEmpty
        @Valid
        List<AmendPrescriptionItemRequest> items,

        @Valid
        List<PrescriptionInteractionOverrideRequest> interactionOverrides

) {
}

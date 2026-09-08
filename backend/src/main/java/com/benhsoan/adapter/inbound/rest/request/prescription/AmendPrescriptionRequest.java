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
        List<PrescriptionInteractionOverrideRequest> interactionOverrides,

        @Valid
        List<PrescriptionAllergyOverrideRequest> allergyOverrides

) {
    public AmendPrescriptionRequest(
            String note,
            String changeReason,
            List<AmendPrescriptionItemRequest> items,
            List<PrescriptionInteractionOverrideRequest> interactionOverrides
    ) {
        this(note, changeReason, items, interactionOverrides, null);
    }
}

package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreatePrescriptionRequest(

        @NotNull
        UUID medicalRecordId,

        String note,

        @NotEmpty
        @Valid
        List<CreatePrescriptionItemRequest> items,

        @Valid
        List<PrescriptionInteractionOverrideRequest> interactionOverrides,

        @Valid
        List<PrescriptionAllergyOverrideRequest> allergyOverrides

) {
    public CreatePrescriptionRequest(
            UUID medicalRecordId,
            String note,
            List<CreatePrescriptionItemRequest> items,
            List<PrescriptionInteractionOverrideRequest> interactionOverrides
    ) {
        this(medicalRecordId, note, items, interactionOverrides, null);
    }
}

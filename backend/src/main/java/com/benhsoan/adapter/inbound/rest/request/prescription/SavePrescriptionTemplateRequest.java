package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SavePrescriptionTemplateRequest(
        @NotNull(message = "Prescription id is required.")
        UUID prescriptionId,

        @NotBlank(message = "Diagnosis code is required.")
        String diagnosisCode
) {
}

package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ApplyPrescriptionTemplateRequest(
        @NotNull(message = "Medical record id is required.")
        UUID medicalRecordId
) {
}

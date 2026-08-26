package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateMedicalRecordTemplateStatusRequest(
        @NotNull Boolean active,
        UUID replacementTemplateId
) {
}

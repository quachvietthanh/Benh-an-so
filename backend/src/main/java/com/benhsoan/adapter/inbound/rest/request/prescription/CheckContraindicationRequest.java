package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CheckContraindicationRequest(
        @NotNull(message = "medicalRecordId is required") UUID medicalRecordId,
        @NotEmpty(message = "medicineIds is required") List<UUID> medicineIds
) {
}

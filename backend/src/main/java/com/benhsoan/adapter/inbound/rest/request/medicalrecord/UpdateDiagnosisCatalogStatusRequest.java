package com.benhsoan.adapter.inbound.rest.request.medicalrecord;

import jakarta.validation.constraints.NotNull;

public record UpdateDiagnosisCatalogStatusRequest(
        @NotNull(message = "Diagnosis catalog active status is required.")
        Boolean active
) {
}

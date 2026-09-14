package com.benhsoan.adapter.inbound.rest.request.clinical;

import jakarta.validation.constraints.NotNull;

public record UpdateClinicalServiceStatusRequest(
        @NotNull(message = "Clinical service active status is required.")
        Boolean active
) {
}

package com.benhsoan.adapter.inbound.rest.request.anonymization;

import jakarta.validation.constraints.NotNull;

public record UpdateAnonymizationModeRequest(
        @NotNull(message = "enabled is required") Boolean enabled
) {
}

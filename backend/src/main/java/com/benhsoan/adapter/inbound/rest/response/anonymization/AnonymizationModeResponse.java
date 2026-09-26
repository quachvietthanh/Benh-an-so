package com.benhsoan.adapter.inbound.rest.response.anonymization;

import java.time.Instant;

public record AnonymizationModeResponse(
        boolean enabled,
        Instant updatedAt
) {
}

package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.UUID;

public record DiagnosisCatalogResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

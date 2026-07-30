package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record DiagnosisCatalogResult(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

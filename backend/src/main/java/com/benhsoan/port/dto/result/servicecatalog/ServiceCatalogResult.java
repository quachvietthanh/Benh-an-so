package com.benhsoan.port.dto.result.servicecatalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServiceCatalogResult(
        UUID id,
        String serviceCode,
        String serviceName,
        boolean active,
        BigDecimal price,
        LocalDate effectiveFrom,
        Instant createdAt,
        Instant updatedAt
) {
}

package com.benhsoan.port.dto.result.servicecatalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServicePriceResult(
        UUID id,
        UUID serviceCatalogId,
        BigDecimal price,
        LocalDate effectiveFrom,
        Instant createdAt,
        UUID createdBy
) {
}

package com.benhsoan.adapter.inbound.rest.response.servicecatalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ServicePriceResponse(
        UUID id,
        BigDecimal price,
        LocalDate effectiveFrom,
        Instant createdAt,
        UUID createdBy
) {
}

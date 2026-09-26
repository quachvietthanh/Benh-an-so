package com.benhsoan.adapter.inbound.rest.response.servicecatalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ServiceCatalogResponse(
        UUID id,
        String serviceCode,
        String name,
        BigDecimal price,
        LocalDate effectiveFrom,
        boolean active
) {
}

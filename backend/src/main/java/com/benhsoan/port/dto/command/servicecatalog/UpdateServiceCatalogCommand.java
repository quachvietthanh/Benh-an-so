package com.benhsoan.port.dto.command.servicecatalog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateServiceCatalogCommand(
        UUID serviceCatalogId,
        String serviceName,
        boolean active,
        BigDecimal price,
        LocalDate effectiveFrom
) {
}

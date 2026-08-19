package com.benhsoan.port.dto.command.servicecatalog;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateServiceCatalogCommand(
        String serviceCode,
        String serviceName,
        BigDecimal price,
        LocalDate effectiveFrom
) {
}

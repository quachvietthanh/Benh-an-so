package com.benhsoan.port.dto.command.servicecatalog;

import org.springframework.data.domain.Pageable;

public record SearchServiceCatalogQuery(
        String keyword,
        Boolean active,
        Pageable pageable
) {
}

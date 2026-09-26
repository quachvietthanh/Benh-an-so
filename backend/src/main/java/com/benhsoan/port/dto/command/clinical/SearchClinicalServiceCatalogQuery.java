package com.benhsoan.port.dto.command.clinical;

import com.benhsoan.domain.shared.exception.ValidationException;

public record SearchClinicalServiceCatalogQuery(String keyword, int page, int size) {

    public SearchClinicalServiceCatalogQuery {
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Page must be non-negative and size must be between 1 and 100.");
        }
    }
}

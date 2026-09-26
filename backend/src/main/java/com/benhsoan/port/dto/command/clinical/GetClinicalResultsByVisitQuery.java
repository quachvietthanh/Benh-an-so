package com.benhsoan.port.dto.command.clinical;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

public record GetClinicalResultsByVisitQuery(UUID visitId, int page, int size) {

    public GetClinicalResultsByVisitQuery {
        if (visitId == null) {
            throw new ValidationException("Visit id is required.");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Page must be non-negative and size must be between 1 and 100.");
        }
    }
}

package com.benhsoan.port.dto.command.prescription;

import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.shared.exception.ValidationException;

public record SearchPrescriptionsQuery(
        PrescriptionStatus status,
        int page,
        int size
) {

    public SearchPrescriptionsQuery {
        if (status == null) {
            throw new ValidationException("Prescription status is required.");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Page must be non-negative and size must be between 1 and 100.");
        }
    }
}

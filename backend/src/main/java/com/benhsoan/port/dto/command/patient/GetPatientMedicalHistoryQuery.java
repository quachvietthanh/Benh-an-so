package com.benhsoan.port.dto.command.patient;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

public record GetPatientMedicalHistoryQuery(
        UUID patientId,
        Instant from,
        Instant to,
        int page,
        int size
) {

    public GetPatientMedicalHistoryQuery {
        if (patientId == null) {
            throw new ValidationException("Patient id is required.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("From time must not be after to time.");
        }
        if (page < 0) {
            throw new ValidationException("Page must not be negative.");
        }
        if (size < 1 || size > 100) {
            throw new ValidationException("Size must be between 1 and 100.");
        }
    }
}

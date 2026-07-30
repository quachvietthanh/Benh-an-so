package com.benhsoan.port.dto.command.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

public record GetMedicalRecordAccessLogsQuery(
        UUID medicalRecordId, UUID patientId, Instant from, Instant to, int page, int size
) {
    public GetMedicalRecordAccessLogsQuery {
        if ((medicalRecordId == null) == (patientId == null)) {
            throw new ValidationException("Exactly one of medical record id or patient id is required.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("From time must not be after to time.");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("Page must be non-negative and size must be between 1 and 100.");
        }
    }
}

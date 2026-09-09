package com.benhsoan.port.dto.command.prescription;

import java.time.Instant;
import java.util.UUID;

public record SearchPrescriptionAllergyWarningLogsQuery(
        UUID doctorId,
        UUID patientId,
        Instant from,
        Instant to,
        int page,
        int size
) {
    public SearchPrescriptionAllergyWarningLogsQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20;
        }
    }
}

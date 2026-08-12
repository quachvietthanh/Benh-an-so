package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record PayableEncounterResult(
        UUID visitId,
        String visitCode,
        UUID patientId,
        String patientCode,
        String patientName,
        String reason,
        Instant completedAt
) {
}

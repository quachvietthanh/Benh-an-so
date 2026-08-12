package com.benhsoan.port.outbound.repository.billing;

import java.time.Instant;
import java.util.UUID;

public record PayableEncounterSummary(
        UUID visitId,
        String visitCode,
        UUID patientId,
        String patientCode,
        String patientName,
        String reason,
        Instant completedAt
) {
}

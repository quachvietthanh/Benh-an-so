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
        Instant completedAt,
        boolean hasPrescription,
        boolean hasPendingDispense
) {
    public PayableEncounterSummary(
            UUID visitId,
            String visitCode,
            UUID patientId,
            String patientCode,
            String patientName,
            String reason,
            Instant completedAt
    ) {
        this(visitId, visitCode, patientId, patientCode, patientName, reason, completedAt, false, false);
    }
}

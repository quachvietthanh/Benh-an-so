package com.benhsoan.adapter.inbound.rest.response.billing;

import java.time.Instant;
import java.util.UUID;

public record PayableEncounterResponse(
        UUID visitId,
        String visitCode,
        UUID patientId,
        String patientCode,
        String patientName,
        String reason,
        Instant completedAt
) {
}

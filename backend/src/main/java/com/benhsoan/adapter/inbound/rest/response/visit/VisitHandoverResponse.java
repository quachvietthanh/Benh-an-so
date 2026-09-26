package com.benhsoan.adapter.inbound.rest.response.visit;

import java.time.Instant;
import java.util.UUID;

public record VisitHandoverResponse(
        UUID id,
        UUID visitId,
        UUID fromDoctorId,
        String fromDoctorName,
        UUID toDoctorId,
        String toDoctorName,
        String reason,
        Instant handedOverAt
) {
}

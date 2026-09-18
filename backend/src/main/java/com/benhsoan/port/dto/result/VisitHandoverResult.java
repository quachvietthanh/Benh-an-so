package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record VisitHandoverResult(
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

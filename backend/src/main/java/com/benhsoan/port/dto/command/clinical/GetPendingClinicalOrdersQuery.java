package com.benhsoan.port.dto.command.clinical;

import java.time.Instant;
import java.util.UUID;

public record GetPendingClinicalOrdersQuery(
        UUID patientId,
        UUID doctorId,
        Instant fromDate,
        Instant toDate,
        int page,
        int size
) {
    public GetPendingClinicalOrdersQuery {
        page = Math.max(0, page);
        size = size <= 0 ? 20 : size;
    }
}

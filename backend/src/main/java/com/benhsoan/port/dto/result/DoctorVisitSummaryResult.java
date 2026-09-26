package com.benhsoan.port.dto.result;

import java.util.UUID;

public record DoctorVisitSummaryResult(
        Integer rank,
        UUID doctorId,
        String doctorCode,
        String doctorName,
        long totalVisits
) {
}

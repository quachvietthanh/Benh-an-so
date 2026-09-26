package com.benhsoan.port.outbound.repository.reporting;

import java.util.UUID;

public record DoctorVisitSummary(
        UUID doctorId,
        String doctorCode,
        String doctorName,
        long totalVisits
) {
}

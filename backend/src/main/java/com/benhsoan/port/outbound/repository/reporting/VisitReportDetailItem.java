package com.benhsoan.port.outbound.repository.reporting;

import java.time.Instant;
import java.util.UUID;

public record VisitReportDetailItem(
        UUID visitId,
        String visitCode,
        Instant completedAt,
        UUID patientId,
        String patientCode,
        String patientFullName,
        String patientPhone,
        String patientAddress,
        UUID doctorId,
        String doctorFullName,
        String status
) {
}

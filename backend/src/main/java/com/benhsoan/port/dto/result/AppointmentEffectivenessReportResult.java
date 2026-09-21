package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AppointmentEffectivenessReportResult(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        long total,
        List<AppointmentStatusCountResult> items
) {
}

package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AppointmentEffectivenessReportResponse(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        long total,
        List<AppointmentStatusCountResponse> items
) {
}

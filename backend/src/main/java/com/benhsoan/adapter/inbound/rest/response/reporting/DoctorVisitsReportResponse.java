package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DoctorVisitsReportResponse(
        LocalDate from,
        LocalDate to,
        Instant generatedAt,
        List<DoctorVisitItemResponse> items
) {
}

package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DiseasePatternReportResponse(
        LocalDate from,
        LocalDate to,
        UUID doctorId,
        String doctorName,
        long totalDiagnoses,
        Instant generatedAt,
        List<DiseasePatternItemResponse> items
) {
}

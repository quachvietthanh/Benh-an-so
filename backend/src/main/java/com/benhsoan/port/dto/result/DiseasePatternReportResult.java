package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DiseasePatternReportResult(
        LocalDate from,
        LocalDate to,
        UUID doctorId,
        String doctorName,
        long totalDiagnoses,
        Instant generatedAt,
        List<DiseasePatternItemResult> items
) {
}

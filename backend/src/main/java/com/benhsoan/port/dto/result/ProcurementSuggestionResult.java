package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProcurementSuggestionResult(
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        Instant calculatedAt,
        int totalItems,
        List<ProcurementSuggestionItemResult> items
) {
}

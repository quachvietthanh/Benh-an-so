package com.benhsoan.adapter.inbound.rest.response.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProcurementSuggestionResponse(
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        Instant calculatedAt,
        int totalItems,
        List<ProcurementSuggestionItemResponse> items
) {
}

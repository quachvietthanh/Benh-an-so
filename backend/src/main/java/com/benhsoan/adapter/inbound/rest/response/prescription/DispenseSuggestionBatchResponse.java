package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.time.LocalDate;
import java.util.UUID;

public record DispenseSuggestionBatchResponse(
        UUID batchId,
        String batchNumber,
        LocalDate expiryDate,
        int availableQuantity,
        int suggestedQuantity
) {
}

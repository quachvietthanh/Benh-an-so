package com.benhsoan.port.dto.result;

import java.time.LocalDate;
import java.util.UUID;

public record DispenseSuggestionBatchResult(
        UUID batchId,
        String batchNumber,
        LocalDate expiryDate,
        int availableQuantity,
        int suggestedQuantity
) {
}

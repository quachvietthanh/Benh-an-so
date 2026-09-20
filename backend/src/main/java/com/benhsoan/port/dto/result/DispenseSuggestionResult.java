package com.benhsoan.port.dto.result;

import java.util.List;
import java.util.UUID;

public record DispenseSuggestionResult(
        UUID prescriptionId,
        List<DispenseSuggestionItemResult> items
) {
}

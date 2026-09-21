package com.benhsoan.port.dto.result;

import java.util.List;
import java.util.UUID;

public record DispenseSuggestionItemResult(
        UUID prescriptionItemId,
        UUID medicineId,
        String medicineName,
        int prescribedQuantity,
        int remainingQuantity,
        List<DispenseSuggestionBatchResult> batches
) {
}

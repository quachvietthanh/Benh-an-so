package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.List;
import java.util.UUID;

public record DispenseSuggestionItemResponse(
        UUID prescriptionItemId,
        UUID medicineId,
        String medicineName,
        int prescribedQuantity,
        int remainingQuantity,
        List<DispenseSuggestionBatchResponse> batches
) {
}

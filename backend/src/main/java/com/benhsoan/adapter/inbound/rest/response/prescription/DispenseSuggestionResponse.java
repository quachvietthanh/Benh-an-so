package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.List;
import java.util.UUID;

public record DispenseSuggestionResponse(
        UUID prescriptionId,
        List<DispenseSuggestionItemResponse> items
) {
}

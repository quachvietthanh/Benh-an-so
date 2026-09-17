package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.List;

import jakarta.validation.Valid;

public record PartialDispensePrescriptionRequest(
        @Valid List<DispenseItemRequest> items
) {
    public PartialDispensePrescriptionRequest {
        items = items == null ? List.of() : items;
    }
}
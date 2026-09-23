package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.List;

import jakarta.validation.Valid;

public record PartialDispensePrescriptionRequest(
        @Valid List<DispenseItemRequest> items,
        boolean controlledMedicineConfirmed
) {
    public PartialDispensePrescriptionRequest {
        items = items == null ? List.of() : items;
    }
}
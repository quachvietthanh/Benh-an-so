package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PartialDispensePrescriptionResponse(
        PrescriptionResponse prescription,
        UUID dispensedBy,
        Instant dispensedAt,
        List<DispenseItemSummaryResponse> items,
        List<DispenseAllocationResponse> allocations
) {
}
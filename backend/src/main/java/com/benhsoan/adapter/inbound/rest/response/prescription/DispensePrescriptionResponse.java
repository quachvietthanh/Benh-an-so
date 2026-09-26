package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DispensePrescriptionResponse(
        PrescriptionResponse prescription,
        UUID dispensedBy,
        Instant dispensedAt,
        int allocationCount,
        int totalDispensedQuantity,
        List<DispenseAllocationResponse> allocations
) {
}

package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DispensePrescriptionResult(
        PrescriptionResult prescription,
        UUID dispensedBy,
        Instant dispensedAt,
        int allocationCount,
        int totalDispensedQuantity,
        List<DispenseAllocationResult> allocations
) {
}

package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PartialDispensePrescriptionResult(
        PrescriptionResult prescription,
        UUID dispensedBy,
        Instant dispensedAt,
        List<DispenseItemSummaryResult> items,
        List<DispenseAllocationResult> allocations
) {
}
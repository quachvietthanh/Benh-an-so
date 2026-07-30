package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClinicalOrderResult(
        UUID id,
        String orderCode,
        UUID visitId,
        UUID patientId,
        UUID orderedBy,
        String clinicalReason,
        String status,
        Instant orderedAt,
        Instant completedAt,
        List<OrderItemResult> items
) {
    public record OrderItemResult(
            UUID id,
            String serviceCode,
            String serviceName,
            String instruction,
            String status
    ) {}
}

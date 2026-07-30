package com.benhsoan.adapter.inbound.rest.response.clinical;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClinicalOrderResponse(
        UUID id,
        String orderCode,
        UUID visitId,
        UUID patientId,
        UUID orderedBy,
        String clinicalReason,
        String status,
        Instant orderedAt,
        Instant completedAt,
        List<OrderItemResponse> items
) {
    public record OrderItemResponse(
            UUID id,
            String serviceCode,
            String serviceName,
            String instruction,
            String status
    ) {}
}

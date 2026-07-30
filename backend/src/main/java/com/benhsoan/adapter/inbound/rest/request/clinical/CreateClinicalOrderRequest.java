package com.benhsoan.adapter.inbound.rest.request.clinical;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record CreateClinicalOrderRequest(
        String clinicalReason,
        @NotEmpty List<OrderItem> items
) {
    public record OrderItem(
            UUID serviceId,
            @NotEmpty String serviceCode,
            @NotEmpty String serviceName,
            String instruction
    ) {}
}

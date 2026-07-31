package com.benhsoan.adapter.inbound.rest.request.clinical;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateClinicalOrderRequest(
        String clinicalReason,
        @NotEmpty List<@Valid OrderItem> items
) {
    public record OrderItem(
            @NotNull UUID serviceId,
            String instruction
    ) {}
}

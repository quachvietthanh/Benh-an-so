package com.benhsoan.adapter.inbound.rest.request.queue;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckInWalkInRequest(
        @NotNull UUID patientId,
        @NotNull UUID doctorId,
        @NotBlank String reason,
        String note
) {
}

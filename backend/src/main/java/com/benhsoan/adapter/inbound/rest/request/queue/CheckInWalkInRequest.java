package com.benhsoan.adapter.inbound.rest.request.queue;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckInWalkInRequest(
        @NotNull UUID patientId,
        @NotNull UUID doctorId,
        @NotBlank String reason,
        String note,
        UUID specialtyId
) {
    public CheckInWalkInRequest(UUID patientId, UUID doctorId, String reason, String note) {
        this(patientId, doctorId, reason, note, null);
    }
}

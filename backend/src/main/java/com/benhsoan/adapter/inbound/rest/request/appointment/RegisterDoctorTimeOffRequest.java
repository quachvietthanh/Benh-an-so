package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDoctorTimeOffRequest(
        @NotNull(message = "doctorId is required.")
        UUID doctorId,

        @NotNull(message = "startTime is required.")
        Instant startTime,

        @NotNull(message = "endTime is required.")
        Instant endTime,

        @Size(max = 255, message = "reason must not exceed 255 characters.")
        String reason
) {
}

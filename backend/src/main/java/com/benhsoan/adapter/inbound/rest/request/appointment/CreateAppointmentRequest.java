package com.benhsoan.adapter.inbound.rest.request.appointment;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAppointmentRequest(

        @NotNull
        UUID patientId,

        @NotNull
        UUID doctorId,

        @NotNull
        @Future
        Instant startTime,

        @NotNull
        @Future
        Instant endTime,

        @NotBlank
        String reason

) {
}

package com.benhsoan.port.dto.command.appointment;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record CreateAppointmentCommand(

        UUID patientId,

        UUID doctorId,

        Instant startTime,

        Instant endTime,

        String reason

) {
}
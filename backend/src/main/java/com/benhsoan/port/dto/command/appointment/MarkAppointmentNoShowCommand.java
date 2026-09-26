package com.benhsoan.port.dto.command.appointment;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record MarkAppointmentNoShowCommand(
        UUID appointmentId,
        Instant markedAt
        ) {

}

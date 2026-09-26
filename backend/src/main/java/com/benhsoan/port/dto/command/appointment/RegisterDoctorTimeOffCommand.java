package com.benhsoan.port.dto.command.appointment;

import java.time.Instant;
import java.util.UUID;

public record RegisterDoctorTimeOffCommand(
        UUID doctorId,
        Instant startTime,
        Instant endTime,
        String reason
) {
}

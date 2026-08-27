package com.benhsoan.port.dto.result.appointment;

import java.time.Instant;

public record DoctorAvailableSlotResult(
        Instant startTime,
        Instant endTime,
        boolean isAvailable
) {
}

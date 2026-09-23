package com.benhsoan.domain.appointment;

import java.time.Instant;

public record AppointmentSeriesConflictDetail(
        int sequenceNumber,
        Instant startTime,
        Instant endTime,
        String conflictType,
        String reason
) {
}

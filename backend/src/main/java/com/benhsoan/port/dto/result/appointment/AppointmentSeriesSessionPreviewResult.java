package com.benhsoan.port.dto.result.appointment;

import java.time.Instant;

import lombok.Builder;

@Builder
public record AppointmentSeriesSessionPreviewResult(
        int sequenceNumber,
        Instant startTime,
        Instant endTime,
        String status,
        String conflictReason
) {
}

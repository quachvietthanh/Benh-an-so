package com.benhsoan.adapter.inbound.rest.response.appointment;

import java.time.Instant;

import lombok.Builder;

@Builder
public record AppointmentSeriesSessionPreviewResponse(
        int sequenceNumber,
        Instant startTime,
        Instant endTime,
        String status,
        String conflictReason
) {
}

package com.benhsoan.adapter.inbound.rest.response.appointment;

import java.util.List;

import lombok.Builder;

@Builder
public record AppointmentSeriesPreviewResponse(
        int totalSessions,
        int intervalDays,
        boolean allAvailable,
        int conflictCount,
        List<AppointmentSeriesSessionPreviewResponse> sessions
) {
}

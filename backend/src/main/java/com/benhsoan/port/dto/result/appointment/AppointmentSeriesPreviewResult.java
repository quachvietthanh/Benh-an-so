package com.benhsoan.port.dto.result.appointment;

import java.util.List;

import lombok.Builder;

@Builder
public record AppointmentSeriesPreviewResult(
        int totalSessions,
        int intervalDays,
        boolean allAvailable,
        int conflictCount,
        List<AppointmentSeriesSessionPreviewResult> sessions
) {
}

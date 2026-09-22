package com.benhsoan.port.dto.command.appointment;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record PreviewAppointmentSeriesCommand(
        UUID patientId,
        UUID doctorId,
        Instant firstSessionStartTime,
        int sessionDurationMinutes,
        int totalSessions,
        int intervalDays
) {
}

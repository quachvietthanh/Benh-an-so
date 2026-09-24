package com.benhsoan.port.dto.command.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder
public record CreateAppointmentSeriesCommand(
        UUID patientId,
        UUID doctorId,
        UUID medicalRecordId,
        String title,
        String notes,
        int totalSessions,
        int intervalDays,
        List<AppointmentSeriesSessionCommand> sessions
) {
    public record AppointmentSeriesSessionCommand(
            int sequenceNumber,
            Instant startTime,
            Instant endTime
    ) {
    }
}

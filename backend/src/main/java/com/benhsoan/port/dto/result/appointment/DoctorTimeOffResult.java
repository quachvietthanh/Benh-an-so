package com.benhsoan.port.dto.result.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.TimeOffStatus;

public record DoctorTimeOffResult(
        UUID id,
        UUID doctorId,
        Instant startTime,
        Instant endTime,
        String reason,
        TimeOffStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<AffectedAppointmentResult> affectedAppointments
) {
}

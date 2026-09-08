package com.benhsoan.adapter.inbound.rest.response.appointment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.TimeOffStatus;

public record DoctorTimeOffResponse(
        UUID id,
        UUID doctorId,
        Instant startTime,
        Instant endTime,
        String reason,
        TimeOffStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<AffectedAppointmentResponse> affectedAppointments
) {
}

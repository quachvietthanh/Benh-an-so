package com.benhsoan.port.dto.result.appointment;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

public record AffectedAppointmentResult(
        UUID id,
        String appointmentCode,
        UUID patientId,
        Instant startTime,
        Instant endTime,
        AppointmentStatus status,
        String reason
) {
}

package com.benhsoan.adapter.inbound.rest.response.appointment;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

public record AffectedAppointmentResponse(
        UUID appointmentId,
        String appointmentCode,
        UUID patientId,
        String patientName,
        String patientPhone,
        Instant startTime,
        Instant endTime,
        AppointmentStatus status,
        String reason
) {
}

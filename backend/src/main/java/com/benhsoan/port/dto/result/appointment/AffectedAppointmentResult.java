package com.benhsoan.port.dto.result.appointment;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

public record AffectedAppointmentResult(
        UUID id,
        String appointmentCode,
        UUID patientId,
        String patientFullName,
        String patientPhone,
        Instant startTime,
        Instant endTime,
        AppointmentStatus status,
        String reason
) {
}

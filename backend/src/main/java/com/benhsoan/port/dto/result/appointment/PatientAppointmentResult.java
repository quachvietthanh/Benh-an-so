package com.benhsoan.port.dto.result.appointment;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

public record PatientAppointmentResult(
        UUID id,
        String appointmentCode,
        UUID patientId,
        UUID doctorId,
        Instant startTime,
        Instant endTime,
        AppointmentStatus status,
        String reason,
        String bookingChannel,
        Instant createdAt,
        Instant confirmedAt
) {
    public PatientAppointmentResult(
            UUID id,
            String appointmentCode,
            UUID patientId,
            UUID doctorId,
            Instant startTime,
            Instant endTime,
            AppointmentStatus status,
            String reason,
            String bookingChannel,
            Instant createdAt
    ) {
        this(id, appointmentCode, patientId, doctorId, startTime, endTime, status, reason, bookingChannel, createdAt, null);
    }
}

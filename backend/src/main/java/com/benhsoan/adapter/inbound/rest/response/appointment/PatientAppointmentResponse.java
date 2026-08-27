package com.benhsoan.adapter.inbound.rest.response.appointment;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

public record PatientAppointmentResponse(

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
}

package com.benhsoan.port.dto.command.appointment;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.appointment.enums.AppointmentStatus;

public record SearchAppointmentCommand(

        UUID patientId,

        UUID doctorId,

        AppointmentStatus status,

        Instant startDate,

        Instant endDate,

        Pageable pageable

) {
}
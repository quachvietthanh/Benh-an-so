package com.benhsoan.port.dto.command.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record PatientBookAppointmentCommand(
        UUID doctorId,
        LocalDate appointmentDate,
        LocalTime startTime,
        String reason
) {
}

package com.benhsoan.port.dto.command.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record PatientBookAppointmentCommand(
        UUID doctorId,
        LocalDate appointmentDate,
        LocalTime startTime,
        String reason,
        UUID patientId
) {

    /**
     * NCL-14-CN-003 compatibility form: books for the authenticated user's own patient
     * profile (patientId == null).
     */
    public PatientBookAppointmentCommand(
            UUID doctorId,
            LocalDate appointmentDate,
            LocalTime startTime,
            String reason
    ) {
        this(doctorId, appointmentDate, startTime, reason, null);
    }
}

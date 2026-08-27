package com.benhsoan.port.dto.command.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

public record PatientRescheduleAppointmentCommand(

        LocalDate newAppointmentDate,

        LocalTime newStartTime,

        String reason

) {
}

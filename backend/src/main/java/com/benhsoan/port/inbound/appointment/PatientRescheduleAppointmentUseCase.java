package com.benhsoan.port.inbound.appointment;

import java.util.UUID;

import com.benhsoan.port.dto.command.appointment.PatientRescheduleAppointmentCommand;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;

public interface PatientRescheduleAppointmentUseCase {

    PatientAppointmentResult reschedule(
            UUID appointmentId,
            PatientRescheduleAppointmentCommand command
    );

}

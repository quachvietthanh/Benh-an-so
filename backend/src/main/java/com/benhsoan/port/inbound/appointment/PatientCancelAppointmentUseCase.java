package com.benhsoan.port.inbound.appointment;

import java.util.UUID;

import com.benhsoan.port.dto.command.appointment.PatientCancelAppointmentCommand;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;

public interface PatientCancelAppointmentUseCase {

    PatientAppointmentResult cancel(
            UUID appointmentId,
            PatientCancelAppointmentCommand command
    );

}

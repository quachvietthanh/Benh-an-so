package com.benhsoan.port.inbound.appointment;

import java.util.UUID;

import com.benhsoan.port.dto.command.appointment.CancelAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;

public interface CancelAppointmentUseCase {

    AppointmentResult cancel(
            UUID appointmentId,
            CancelAppointmentCommand command
    );

}
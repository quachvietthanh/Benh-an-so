package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.command.appointment.CreateAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;

public interface CreateAppointmentUseCase {

    AppointmentResult create(CreateAppointmentCommand command);

}
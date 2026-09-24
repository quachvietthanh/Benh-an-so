package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.command.appointment.CreateAppointmentSeriesCommand;
import com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult;

public interface CreateAppointmentSeriesUseCase {

    AppointmentSeriesResult create(CreateAppointmentSeriesCommand command);
}

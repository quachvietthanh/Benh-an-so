package com.benhsoan.port.inbound.appointment;

import java.util.UUID;

import com.benhsoan.port.dto.result.appointment.AppointmentSeriesResult;

public interface GetAppointmentSeriesByIdUseCase {

    AppointmentSeriesResult getById(UUID id);
}

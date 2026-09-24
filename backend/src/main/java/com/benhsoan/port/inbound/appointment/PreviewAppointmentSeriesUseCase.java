package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.command.appointment.PreviewAppointmentSeriesCommand;
import com.benhsoan.port.dto.result.appointment.AppointmentSeriesPreviewResult;

public interface PreviewAppointmentSeriesUseCase {

    AppointmentSeriesPreviewResult preview(PreviewAppointmentSeriesCommand command);
}

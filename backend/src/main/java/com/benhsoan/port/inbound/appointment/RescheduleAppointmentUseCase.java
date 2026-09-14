package com.benhsoan.port.inbound.appointment;

import java.util.UUID;

import com.benhsoan.port.dto.command.appointment.RescheduleAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;

public interface RescheduleAppointmentUseCase {

    AppointmentResult reschedule(UUID appointmentId, RescheduleAppointmentCommand command);

}

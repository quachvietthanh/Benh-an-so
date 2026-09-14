package com.benhsoan.port.inbound.appointment;

import java.util.UUID;

import com.benhsoan.port.dto.result.AppointmentResult;

public interface ConfirmAppointmentUseCase {

    AppointmentResult confirm(UUID appointmentId);

}

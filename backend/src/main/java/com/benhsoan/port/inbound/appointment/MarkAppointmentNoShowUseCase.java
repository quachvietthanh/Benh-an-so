package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.command.appointment.MarkAppointmentNoShowCommand;
import com.benhsoan.port.dto.result.AppointmentResult;

public interface MarkAppointmentNoShowUseCase {

    AppointmentResult execute(
            MarkAppointmentNoShowCommand command
    );

}

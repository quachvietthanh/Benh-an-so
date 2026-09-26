package com.benhsoan.port.inbound.appointment;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.appointment.GetOverdueAppointmentsCommand;
import com.benhsoan.port.dto.result.AppointmentResult;

public interface GetOverdueAppointmentsUseCase {

    Page<AppointmentResult> execute(
            GetOverdueAppointmentsCommand command
    );

}

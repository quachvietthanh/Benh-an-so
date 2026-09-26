package com.benhsoan.port.inbound.appointment;

import org.springframework.data.domain.Page;

import com.benhsoan.port.dto.command.appointment.SearchAppointmentCommand;
import com.benhsoan.port.dto.result.AppointmentResult;

public interface SearchAppointmentsUseCase {

    Page<AppointmentResult> search(SearchAppointmentCommand command);
}

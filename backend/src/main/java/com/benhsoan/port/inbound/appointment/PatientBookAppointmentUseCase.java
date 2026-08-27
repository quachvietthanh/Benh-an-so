package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.command.appointment.PatientBookAppointmentCommand;
import com.benhsoan.port.dto.result.appointment.PatientAppointmentResult;

public interface PatientBookAppointmentUseCase {

    PatientAppointmentResult book(PatientBookAppointmentCommand command);

}

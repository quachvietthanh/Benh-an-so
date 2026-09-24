package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.command.appointment.AddToWaitlistCommand;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;

public interface AddToWaitlistUseCase {
    AppointmentWaitlistResult addToWaitlist(AddToWaitlistCommand command);
}

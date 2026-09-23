package com.benhsoan.port.inbound.appointment;

import com.benhsoan.port.dto.command.appointment.CancelWaitlistEntryCommand;
import com.benhsoan.port.dto.result.appointment.AppointmentWaitlistResult;

public interface CancelWaitlistEntryUseCase {
    AppointmentWaitlistResult cancel(CancelWaitlistEntryCommand command);
}

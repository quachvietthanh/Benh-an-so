package com.benhsoan.port.inbound.queue;

import com.benhsoan.port.dto.command.queue.CheckInAppointmentCommand;
import com.benhsoan.port.dto.result.QueueCheckInResult;

public interface CheckInAppointmentUseCase {

    QueueCheckInResult checkIn(CheckInAppointmentCommand command);
}

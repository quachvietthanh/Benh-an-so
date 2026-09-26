package com.benhsoan.port.inbound.queue;

import com.benhsoan.port.dto.command.queue.CheckInWalkInCommand;
import com.benhsoan.port.dto.result.QueueCheckInResult;

public interface CheckInWalkInUseCase {

    QueueCheckInResult checkIn(CheckInWalkInCommand command);
}

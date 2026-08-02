package com.benhsoan.port.inbound.queue;

import com.benhsoan.port.dto.command.queue.UpdateRoomCommand;
import com.benhsoan.port.dto.result.RoomResult;

public interface UpdateRoomUseCase {

    RoomResult update(UpdateRoomCommand command);
}

package com.benhsoan.port.inbound.queue;

import com.benhsoan.port.dto.command.queue.CreateRoomCommand;
import com.benhsoan.port.dto.result.RoomResult;

public interface CreateRoomUseCase {

    RoomResult create(CreateRoomCommand command);
}

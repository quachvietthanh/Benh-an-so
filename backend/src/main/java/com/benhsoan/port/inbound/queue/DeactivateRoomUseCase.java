package com.benhsoan.port.inbound.queue;

import java.util.UUID;

import com.benhsoan.port.dto.result.RoomResult;

public interface DeactivateRoomUseCase {

    RoomResult deactivate(UUID roomId);
}

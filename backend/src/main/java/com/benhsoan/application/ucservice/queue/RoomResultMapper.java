package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.queue.Room;
import com.benhsoan.port.dto.result.RoomResult;

@Component
class RoomResultMapper {

    RoomResult toResult(Room room) {
        return new RoomResult(room.getId(), room.getCode(), room.getName(), room.isActive(), room.getCreatedAt(),
                room.getUpdatedAt());
    }
}

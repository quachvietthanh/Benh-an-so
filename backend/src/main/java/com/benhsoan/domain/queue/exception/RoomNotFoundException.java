package com.benhsoan.domain.queue.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class RoomNotFoundException extends QueueException {

    public RoomNotFoundException(UUID roomId) {
        super(DomainErrorCode.ROOM_NOT_FOUND, "Room does not exist: " + roomId);
    }
}

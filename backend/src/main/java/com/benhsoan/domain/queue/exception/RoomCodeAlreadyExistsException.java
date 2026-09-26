package com.benhsoan.domain.queue.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class RoomCodeAlreadyExistsException extends QueueException {

    public RoomCodeAlreadyExistsException(String roomCode) {
        super(DomainErrorCode.ROOM_CODE_ALREADY_EXISTS, "Room code already exists: " + roomCode);
    }
}

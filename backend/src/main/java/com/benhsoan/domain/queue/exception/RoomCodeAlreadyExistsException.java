package com.benhsoan.domain.queue.exception;


public class RoomCodeAlreadyExistsException extends QueueException {

    public RoomCodeAlreadyExistsException(String roomCode) {
        super("Room code already exists: " + roomCode);
    }
}

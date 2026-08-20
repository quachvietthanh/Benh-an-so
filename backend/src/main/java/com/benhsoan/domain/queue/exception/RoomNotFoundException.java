package com.benhsoan.domain.queue.exception;

import java.util.UUID;


public class RoomNotFoundException extends QueueException {

    public RoomNotFoundException(UUID roomId) {
        super("Room does not exist: " + roomId);
    }
}

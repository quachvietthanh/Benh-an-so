package com.benhsoan.domain.queue.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class RoomCodeAlreadyExistsException extends DomainException {

    public RoomCodeAlreadyExistsException(String roomCode) {
        super(HttpStatus.CONFLICT, "Room code already exists: " + roomCode);
    }
}

package com.benhsoan.domain.queue.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class RoomNotFoundException extends DomainException {

    public RoomNotFoundException(UUID roomId) {
        super(HttpStatus.NOT_FOUND, "Room does not exist: " + roomId);
    }
}

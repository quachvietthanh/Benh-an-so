package com.benhsoan.domain.queue.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class CheckInConflictException extends DomainException {

    public CheckInConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}

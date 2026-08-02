package com.benhsoan.domain.queue.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class UnauthorizedQueueOperationException extends DomainException {

    public UnauthorizedQueueOperationException() {
        super(HttpStatus.FORBIDDEN, "You do not have permission to perform this queue operation.");
    }
}

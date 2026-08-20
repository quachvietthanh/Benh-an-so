package com.benhsoan.domain.queue.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class CheckInConflictException extends QueueException {

    public CheckInConflictException(String message) {
        super(DomainErrorCode.CHECK_IN_CONFLICT, message);
    }
}

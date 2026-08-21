package com.benhsoan.domain.queue.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class UnauthorizedQueueOperationException extends QueueException {

    public UnauthorizedQueueOperationException() {
        super(DomainErrorCode.UNAUTHORIZED_QUEUE_OPERATION, "You do not have permission to perform this queue operation.");
    }
}

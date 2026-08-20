package com.benhsoan.domain.queue.exception;


public class UnauthorizedQueueOperationException extends QueueException {

    public UnauthorizedQueueOperationException() {
        super("You do not have permission to perform this queue operation.");
    }
}

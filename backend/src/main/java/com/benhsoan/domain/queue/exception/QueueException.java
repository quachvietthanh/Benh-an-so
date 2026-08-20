package com.benhsoan.domain.queue.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class QueueException extends DomainException {

    protected QueueException(String message) {
        super(message);
    }
}

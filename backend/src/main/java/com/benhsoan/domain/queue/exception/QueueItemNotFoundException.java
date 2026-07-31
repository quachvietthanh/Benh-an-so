package com.benhsoan.domain.queue.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class QueueItemNotFoundException extends DomainException {

    public QueueItemNotFoundException(UUID queueItemId) {
        super(HttpStatus.NOT_FOUND, "Queue item not found: " + queueItemId);
    }
}

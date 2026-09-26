package com.benhsoan.domain.queue.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class QueueItemNotFoundException extends QueueException {

    public QueueItemNotFoundException(UUID queueItemId) {
        super(DomainErrorCode.QUEUE_ITEM_NOT_FOUND, "Queue item not found: " + queueItemId);
    }
}

package com.benhsoan.domain.queue.exception;

import java.util.UUID;


public class QueueItemNotFoundException extends QueueException {

    public QueueItemNotFoundException(UUID queueItemId) {
        super("Queue item not found: " + queueItemId);
    }
}

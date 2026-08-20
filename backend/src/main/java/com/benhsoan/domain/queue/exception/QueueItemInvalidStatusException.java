package com.benhsoan.domain.queue.exception;


import com.benhsoan.domain.queue.enums.QueueItemStatus;
public class QueueItemInvalidStatusException extends QueueException {

    public QueueItemInvalidStatusException(QueueItemStatus from, QueueItemStatus expected) {
        super(
                "Queue item status " + from + " does not allow this action. Expected " + expected + ".");
    }
}

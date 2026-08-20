package com.benhsoan.domain.queue.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.queue.enums.QueueItemStatus;
public class QueueItemInvalidStatusException extends QueueException {

    public QueueItemInvalidStatusException(QueueItemStatus from, QueueItemStatus expected) {
        super(DomainErrorCode.QUEUE_ITEM_INVALID_STATUS,
                "Queue item status " + from + " does not allow this action. Expected " + expected + ".");
    }
}

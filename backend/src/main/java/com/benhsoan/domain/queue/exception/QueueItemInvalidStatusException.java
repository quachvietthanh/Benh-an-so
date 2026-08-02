package com.benhsoan.domain.queue.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.shared.exception.DomainException;

public class QueueItemInvalidStatusException extends DomainException {

    public QueueItemInvalidStatusException(QueueItemStatus from, QueueItemStatus expected) {
        super(HttpStatus.CONFLICT,
                "Queue item status " + from + " does not allow this action. Expected " + expected + ".");
    }
}

package com.benhsoan.domain.queue.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class QueueNotFoundException
        extends QueueException {

    public QueueNotFoundException() {
        super(DomainErrorCode.QUEUE_NOT_FOUND, "Không tìm thấy hàng đợi");
    }
}

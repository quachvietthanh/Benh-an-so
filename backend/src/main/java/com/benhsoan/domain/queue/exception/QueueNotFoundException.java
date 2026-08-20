package com.benhsoan.domain.queue.exception;

public class QueueNotFoundException
        extends QueueException {

    public QueueNotFoundException() {
        super("Không tìm thấy hàng đợi");
    }
}

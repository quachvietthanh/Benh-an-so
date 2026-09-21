package com.benhsoan.domain.queue.enums;

public enum QueuePriority {
    NORMAL,
    PRIORITY,
    EMERGENCY;

    public boolean isUrgent() {
        return this != NORMAL;
    }
}

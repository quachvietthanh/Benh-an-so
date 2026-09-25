package com.benhsoan.adapter.inbound.rest.response.queue;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.enums.QueuePriority;

public record QueueDisplayItemResponse(
        UUID id,
        int queueNumber,
        String patientInitials,
        QueueItemStatus status,
        QueuePriority priority,
        Instant calledAt
) {}

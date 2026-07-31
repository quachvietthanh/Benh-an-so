package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;

public record QueueItemResult(
        UUID id, UUID medicalQueueId, UUID patientId, UUID appointmentId, UUID visitId,
        QueueItemSourceType sourceType, QueueItemStatus status, int queueNumber, LocalDate queueDate,
        Instant checkedInAt, Instant calledAt, Instant completedAt, Instant cancelledAt, String cancelReason
) {
}

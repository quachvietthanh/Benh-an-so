package com.benhsoan.domain.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;

import lombok.Getter;

@Getter
public class QueueItem {
    private final UUID id, medicalQueueId, patientId, appointmentId, visitId, createdBy;
    private final QueueItemSourceType sourceType;
    private QueueItemStatus status;
    private final int queueNumber;
    private final LocalDate queueDate;
    private final Instant checkedInAt, createdAt;
    private Instant calledAt, completedAt, cancelledAt, updatedAt;
    private String cancelReason;

    private QueueItem(UUID id, UUID medicalQueueId, UUID patientId, UUID appointmentId, UUID visitId,
            QueueItemSourceType sourceType, QueueItemStatus status, int queueNumber, LocalDate queueDate,
            Instant checkedInAt, Instant calledAt, Instant completedAt, Instant cancelledAt, String cancelReason,
            UUID createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id; this.medicalQueueId = medicalQueueId; this.patientId = patientId; this.appointmentId = appointmentId;
        this.visitId = visitId; this.sourceType = sourceType; this.status = status; this.queueNumber = queueNumber;
        this.queueDate = queueDate; this.checkedInAt = checkedInAt; this.calledAt = calledAt; this.completedAt = completedAt;
        this.cancelledAt = cancelledAt; this.cancelReason = cancelReason; this.createdBy = createdBy; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public static QueueItem restore(UUID id, UUID medicalQueueId, UUID patientId, UUID appointmentId, UUID visitId,
            QueueItemSourceType sourceType, QueueItemStatus status, int queueNumber, LocalDate queueDate,
            Instant checkedInAt, Instant calledAt, Instant completedAt, Instant cancelledAt, String cancelReason,
            UUID createdBy, Instant createdAt, Instant updatedAt) {
        return new QueueItem(id, medicalQueueId, patientId, appointmentId, visitId, sourceType, status, queueNumber,
                queueDate, checkedInAt, calledAt, completedAt, cancelledAt, cancelReason, createdBy, createdAt, updatedAt);
    }
}

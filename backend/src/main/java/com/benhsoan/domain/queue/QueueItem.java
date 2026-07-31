package com.benhsoan.domain.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.exception.QueueItemInvalidStatusException;
import com.benhsoan.domain.shared.Guard.Guard;

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
        this.id = Guard.require(id, "Queue item id");
        this.medicalQueueId = Guard.require(medicalQueueId, "Medical queue id");
        this.patientId = Guard.require(patientId, "Patient id");
        this.appointmentId = appointmentId;
        this.visitId = Guard.require(visitId, "Visit id");
        this.sourceType = Guard.require(sourceType, "Source type");
        this.status = Guard.require(status, "Status");
        if (queueNumber < 1) {
            throw new IllegalArgumentException("Queue number must be positive.");
        }
        this.queueNumber = queueNumber;
        this.queueDate = Guard.require(queueDate, "Queue date");
        this.checkedInAt = Guard.require(checkedInAt, "Checked in at");
        this.calledAt = calledAt;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
        this.cancelReason = cancelReason;
        this.createdBy = Guard.require(createdBy, "Created by");
        this.createdAt = Guard.require(createdAt, "Created at");
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public static QueueItem restore(UUID id, UUID medicalQueueId, UUID patientId, UUID appointmentId, UUID visitId,
            QueueItemSourceType sourceType, QueueItemStatus status, int queueNumber, LocalDate queueDate,
            Instant checkedInAt, Instant calledAt, Instant completedAt, Instant cancelledAt, String cancelReason,
            UUID createdBy, Instant createdAt, Instant updatedAt) {
        return new QueueItem(id, medicalQueueId, patientId, appointmentId, visitId, sourceType, status, queueNumber,
                queueDate, checkedInAt, calledAt, completedAt, cancelledAt, cancelReason, createdBy, createdAt, updatedAt);
    }

    public static QueueItem create(UUID medicalQueueId, UUID patientId, UUID appointmentId, UUID visitId,
            QueueItemSourceType sourceType, int queueNumber, LocalDate queueDate, UUID createdBy, Instant checkedInAt) {
        return new QueueItem(UUID.randomUUID(), medicalQueueId, patientId, appointmentId, visitId, sourceType,
                QueueItemStatus.WAITING, queueNumber, queueDate, checkedInAt, null, null, null, null,
                createdBy, checkedInAt, checkedInAt);
    }

    public void call(Instant calledAt) {
        requireStatus(QueueItemStatus.WAITING);
        this.status = QueueItemStatus.IN_PROGRESS;
        this.calledAt = Guard.require(calledAt, "Called at");
        this.updatedAt = calledAt;
    }

    public void waitForResult(Instant updatedAt) {
        requireStatus(QueueItemStatus.IN_PROGRESS);
        this.status = QueueItemStatus.WAITING_FOR_RESULT;
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public void resume(Instant updatedAt) {
        requireStatus(QueueItemStatus.WAITING_FOR_RESULT);
        this.status = QueueItemStatus.IN_PROGRESS;
        this.updatedAt = Guard.require(updatedAt, "Updated at");
    }

    public void complete(Instant completedAt) {
        requireStatus(QueueItemStatus.IN_PROGRESS);
        this.status = QueueItemStatus.COMPLETED;
        this.completedAt = Guard.require(completedAt, "Completed at");
        this.updatedAt = completedAt;
    }

    public void cancel(String cancelReason, Instant cancelledAt) {
        if (status != QueueItemStatus.WAITING && status != QueueItemStatus.WAITING_FOR_RESULT) {
            throw new QueueItemInvalidStatusException(status, QueueItemStatus.CANCELLED);
        }
        this.status = QueueItemStatus.CANCELLED;
        this.cancelReason = Guard.require(cancelReason, "Cancel reason");
        this.cancelledAt = Guard.require(cancelledAt, "Cancelled at");
        this.updatedAt = cancelledAt;
    }

    private void requireStatus(QueueItemStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new QueueItemInvalidStatusException(status, expectedStatus);
        }
    }
}

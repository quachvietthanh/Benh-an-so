package com.benhsoan.persistence.jpaRepository.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;

import com.benhsoan.domain.queue.enums.QueuePriority;

public record QueueItemDetailsProjection(
        UUID id,
        UUID medicalQueueId,
        UUID patientId,
        String patientCode,
        String patientName,
        UUID doctorId,
        String doctorName,
        UUID roomId,
        String roomNumber,
        UUID appointmentId,
        UUID visitId,
        String visitCode,
        QueueItemSourceType sourceType,
        QueueItemStatus status,
        int queueNumber,
        LocalDate queueDate,
        Instant checkedInAt,
        Instant calledAt,
        Instant completedAt,
        Instant cancelledAt,
        String cancelReason,
        Instant skippedAt,
        String skipReason,
        int callCount,
        QueuePriority priority,
        String priorityReason,
        Instant prioritizedAt,
        UUID prioritizedBy
) {
    public QueueItemDetailsProjection(
            UUID id,
            UUID medicalQueueId,
            UUID patientId,
            String patientCode,
            String patientName,
            UUID doctorId,
            String doctorName,
            UUID roomId,
            String roomNumber,
            UUID appointmentId,
            UUID visitId,
            String visitCode,
            QueueItemSourceType sourceType,
            QueueItemStatus status,
            int queueNumber,
            LocalDate queueDate,
            Instant checkedInAt,
            Instant calledAt,
            Instant completedAt,
            Instant cancelledAt,
            String cancelReason,
            Instant skippedAt,
            String skipReason,
            int callCount
    ) {
        this(id, medicalQueueId, patientId, patientCode, patientName, doctorId, doctorName, roomId, roomNumber,
                appointmentId, visitId, visitCode, sourceType, status, queueNumber, queueDate,
                checkedInAt, calledAt, completedAt, cancelledAt, cancelReason,
                skippedAt, skipReason, callCount,
                QueuePriority.NORMAL, null, null, null);
    }
}

package com.benhsoan.adapter.inbound.rest.response.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.enums.VisitStatus;

public record QueueCheckInResponse(
        UUID queueItemId, UUID medicalQueueId, UUID visitId, String visitCode, UUID appointmentId,
        UUID patientId, UUID doctorId, UUID roomId, int queueNumber, LocalDate queueDate,
        QueueItemSourceType sourceType, QueueItemStatus queueItemStatus, VisitStatus visitStatus, Instant checkedInAt
) {
}

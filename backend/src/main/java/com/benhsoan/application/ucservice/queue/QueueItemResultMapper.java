package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.port.dto.result.QueueItemResult;

@Component
class QueueItemResultMapper {

    QueueItemResult toResult(QueueItem item) {
        return new QueueItemResult(item.getId(), item.getMedicalQueueId(), item.getPatientId(), item.getAppointmentId(),
                item.getVisitId(), item.getSourceType(), item.getStatus(), item.getQueueNumber(), item.getQueueDate(),
                item.getCheckedInAt(), item.getCalledAt(), item.getCompletedAt(), item.getCancelledAt(),
                item.getCancelReason());
    }
}

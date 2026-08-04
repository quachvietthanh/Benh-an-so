package com.benhsoan.port.outbound.repository.queue;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
public interface QueueItemRepository {

    QueueItem save(QueueItem queueItem);

    Optional<QueueItem> findByAppointmentId(UUID appointmentId);

    Optional<QueueItem> findByIdForUpdate(UUID id);

    boolean existsByPatientIdAndQueueDateAndStatusIn(UUID patientId, LocalDate queueDate,
            Collection<QueueItemStatus> statuses);

    int findMaxQueueNumber(UUID medicalQueueId);

    Optional<QueueItem> findNextWaitingForUpdate(UUID medicalQueueId);
}

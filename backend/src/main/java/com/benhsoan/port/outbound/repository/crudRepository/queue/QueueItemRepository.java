package com.benhsoan.port.outbound.repository.crudRepository.queue;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface QueueItemRepository extends BaseRepository<QueueItem, UUID> {

    Optional<QueueItem> findByAppointmentId(UUID appointmentId);

    Optional<QueueItem> findByIdForUpdate(UUID id);

    boolean existsByPatientIdAndQueueDateAndStatusIn(UUID patientId, LocalDate queueDate,
            Collection<QueueItemStatus> statuses);

    int findMaxQueueNumber(UUID medicalQueueId);

    List<QueueItem> findQueueBoard(LocalDate queueDate, UUID doctorId, UUID roomId);

    Optional<QueueItem> findNextWaitingForUpdate(UUID medicalQueueId);
}

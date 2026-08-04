package com.benhsoan.port.outbound.repository.queryRepository.queue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.port.dto.result.QueueItemResult;

public interface QueueItemQueryRepository {

    List<QueueItemResult> findQueueBoard(LocalDate queueDate, UUID doctorId, UUID roomId);

    Optional<QueueItemResult> findDetailById(UUID queueItemId);
}

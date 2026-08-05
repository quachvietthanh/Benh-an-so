package com.benhsoan.persistence.adapterRepository.queue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.persistence.jpaRepository.queue.JpaQueueItemRepository;
import com.benhsoan.persistence.jpaRepository.queue.QueueItemDetailsProjection;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QueueItemQueryRepositoryAdapter implements QueueItemQueryRepository {

    private final JpaQueueItemRepository jpaRepository;

    @Override
    public List<QueueItemResult> findQueueBoard(LocalDate queueDate, UUID doctorId, UUID roomId) {
        return jpaRepository.findQueueBoardDetails(queueDate, doctorId, roomId).stream()
                .map(this::toResult)
                .toList();
    }

    @Override
    public Optional<QueueItemResult> findDetailById(UUID queueItemId) {
        return jpaRepository.findQueueItemDetailsById(queueItemId).map(this::toResult);
    }

    private QueueItemResult toResult(QueueItemDetailsProjection projection) {
        return new QueueItemResult(
                projection.id(),
                projection.medicalQueueId(),
                projection.patientId(),
                projection.patientName(),
                projection.doctorId(),
                projection.doctorName(),
                projection.roomId(),
                projection.roomNumber(),
                projection.appointmentId(),
                projection.visitId(),
                projection.visitCode(),
                projection.sourceType(),
                projection.status(),
                projection.queueNumber(),
                projection.queueDate(),
                projection.checkedInAt(),
                projection.calledAt(),
                projection.completedAt(),
                projection.cancelledAt(),
                projection.cancelReason(),
                projection.skippedAt(),
                projection.skipReason()
        );
    }
}

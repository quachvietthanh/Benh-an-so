package com.benhsoan.persistence.jpaRepository.queue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.domain.queue.enums.QueueItemStatus;

import jakarta.persistence.LockModeType;

import com.benhsoan.persistence.entity.queue.QueueItemEntity;

public interface JpaQueueItemRepository extends JpaRepository<QueueItemEntity, UUID> {
    Optional<QueueItemEntity> findByAppointmentId(UUID appointmentId);
    boolean existsByPatientIdAndQueueDate(UUID patientId, LocalDate queueDate);
    boolean existsByPatientIdAndQueueDateAndStatusIn(UUID patientId, LocalDate queueDate, java.util.Collection<QueueItemStatus> statuses);

    @Query("select coalesce(max(item.queueNumber), 0) from QueueItemEntity item where item.medicalQueueId = :medicalQueueId")
    int findMaxQueueNumber(@Param("medicalQueueId") UUID medicalQueueId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from QueueItemEntity item where item.id = :id")
    Optional<QueueItemEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("select item from QueueItemEntity item join MedicalQueueEntity queue on queue.id = item.medicalQueueId "
            + "where queue.queueDate = :queueDate and (:doctorId is null or queue.doctorId = :doctorId) "
            + "and (:roomId is null or queue.roomId = :roomId) order by queue.doctorId, item.queueNumber")
    List<QueueItemEntity> findQueueBoard(@Param("queueDate") LocalDate queueDate, @Param("doctorId") UUID doctorId,
            @Param("roomId") UUID roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from QueueItemEntity item where item.medicalQueueId = :medicalQueueId "
            + "and item.status = 'WAITING' order by item.queueNumber")
    List<QueueItemEntity> findWaitingForUpdate(@Param("medicalQueueId") UUID medicalQueueId);
}

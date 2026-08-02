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

    @Query("""
            select new com.benhsoan.persistence.jpaRepository.queue.QueueItemDetailsProjection(
                item.id, item.medicalQueueId, item.patientId, patient.fullName,
                queue.doctorId, doctor.fullName, queue.roomId, room.code,
                item.appointmentId, item.visitId, visit.visitCode,
                item.sourceType, item.status, item.queueNumber, item.queueDate,
                item.checkedInAt, item.calledAt, item.completedAt, item.cancelledAt, item.cancelReason,
                item.skippedAt, item.skipReason
            )
            from QueueItemEntity item
            join MedicalQueueEntity queue on queue.id = item.medicalQueueId
            join PatientEntity patient on patient.id = item.patientId
            join UserEntity doctor on doctor.id = queue.doctorId
            join RoomEntity room on room.id = queue.roomId
            join VisitEntity visit on visit.id = item.visitId
            where queue.queueDate = :queueDate
              and (:doctorId is null or queue.doctorId = :doctorId)
              and (:roomId is null or queue.roomId = :roomId)
            order by queue.doctorId, item.queueNumber
            """)
    List<QueueItemDetailsProjection> findQueueBoardDetails(
            @Param("queueDate") LocalDate queueDate,
            @Param("doctorId") UUID doctorId,
            @Param("roomId") UUID roomId
    );

    @Query("""
            select new com.benhsoan.persistence.jpaRepository.queue.QueueItemDetailsProjection(
                item.id, item.medicalQueueId, item.patientId, patient.fullName,
                queue.doctorId, doctor.fullName, queue.roomId, room.code,
                item.appointmentId, item.visitId, visit.visitCode,
                item.sourceType, item.status, item.queueNumber, item.queueDate,
                item.checkedInAt, item.calledAt, item.completedAt, item.cancelledAt, item.cancelReason,
                item.skippedAt, item.skipReason
            )
            from QueueItemEntity item
            join MedicalQueueEntity queue on queue.id = item.medicalQueueId
            join PatientEntity patient on patient.id = item.patientId
            join UserEntity doctor on doctor.id = queue.doctorId
            join RoomEntity room on room.id = queue.roomId
            join VisitEntity visit on visit.id = item.visitId
            where item.id = :queueItemId
            """)
    Optional<QueueItemDetailsProjection> findQueueItemDetailsById(@Param("queueItemId") UUID queueItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from QueueItemEntity item where item.medicalQueueId = :medicalQueueId "
            + "and item.status = 'WAITING' order by item.queueNumber")
    List<QueueItemEntity> findWaitingForUpdate(@Param("medicalQueueId") UUID medicalQueueId);
}

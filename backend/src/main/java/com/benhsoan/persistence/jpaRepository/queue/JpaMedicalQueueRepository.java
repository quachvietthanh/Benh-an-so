package com.benhsoan.persistence.jpaRepository.queue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import com.benhsoan.persistence.entity.queue.MedicalQueueEntity;

public interface JpaMedicalQueueRepository extends JpaRepository<MedicalQueueEntity, UUID> {
    Optional<MedicalQueueEntity> findByDoctorIdAndQueueDate(UUID doctorId, LocalDate queueDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select queue from MedicalQueueEntity queue where queue.doctorId = :doctorId and queue.queueDate = :queueDate")
    Optional<MedicalQueueEntity> findByDoctorIdAndQueueDateForUpdate(UUID doctorId, LocalDate queueDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select queue from MedicalQueueEntity queue where queue.id = :medicalQueueId")
    Optional<MedicalQueueEntity> findByIdForUpdate(@Param("medicalQueueId") UUID medicalQueueId);
}

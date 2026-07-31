package com.benhsoan.persistence.jpaRepository.queue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.queue.QueueItemEntity;

public interface JpaQueueItemRepository extends JpaRepository<QueueItemEntity, UUID> {
    Optional<QueueItemEntity> findByAppointmentId(UUID appointmentId);
    boolean existsByPatientIdAndQueueDate(UUID patientId, LocalDate queueDate);
}

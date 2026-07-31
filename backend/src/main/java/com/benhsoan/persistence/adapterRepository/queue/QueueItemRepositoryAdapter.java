package com.benhsoan.persistence.adapterRepository.queue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.persistence.jpaRepository.queue.JpaQueueItemRepository;
import com.benhsoan.persistence.mapper.queue.QueueStructurePersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.queue.QueueItemRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QueueItemRepositoryAdapter implements QueueItemRepository {
    private final JpaQueueItemRepository jpaRepository;
    private final QueueStructurePersistenceMapper mapper;
    public Optional<QueueItem> findByAppointmentId(UUID appointmentId) { return jpaRepository.findByAppointmentId(appointmentId).map(mapper::toDomain); }
    public boolean existsByPatientIdAndQueueDate(UUID patientId, LocalDate queueDate) { return jpaRepository.existsByPatientIdAndQueueDate(patientId, queueDate); }
}

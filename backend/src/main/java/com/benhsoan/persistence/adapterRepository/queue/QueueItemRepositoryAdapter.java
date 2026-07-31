package com.benhsoan.persistence.adapterRepository.queue;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.persistence.jpaRepository.queue.JpaQueueItemRepository;
import com.benhsoan.persistence.mapper.queue.QueueStructurePersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.queue.QueueItemRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QueueItemRepositoryAdapter implements QueueItemRepository {

    private final JpaQueueItemRepository jpaRepository;
    private final QueueStructurePersistenceMapper mapper;

    @Override
    public Optional<QueueItem> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<QueueItem> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public QueueItem save(QueueItem item) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(item)));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<QueueItem> findByAppointmentId(UUID appointmentId) {
        return jpaRepository.findByAppointmentId(appointmentId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByPatientIdAndQueueDateAndStatusIn(UUID patientId, LocalDate queueDate,
            Collection<QueueItemStatus> statuses) {
        return jpaRepository.existsByPatientIdAndQueueDateAndStatusIn(patientId, queueDate, statuses);
    }

    @Override
    public int findMaxQueueNumber(UUID medicalQueueId) {
        return jpaRepository.findMaxQueueNumber(medicalQueueId);
    }

    @Override
    public List<QueueItem> findQueueBoard(LocalDate queueDate, UUID doctorId, UUID roomId) {
        return jpaRepository.findQueueBoard(queueDate, doctorId, roomId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<QueueItem> findNextWaitingForUpdate(UUID medicalQueueId) {
        return jpaRepository.findWaitingForUpdate(medicalQueueId).stream().findFirst().map(mapper::toDomain);
    }
}

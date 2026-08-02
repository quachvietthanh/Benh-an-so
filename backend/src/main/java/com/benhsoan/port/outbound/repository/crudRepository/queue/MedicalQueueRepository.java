package com.benhsoan.port.outbound.repository.crudRepository.queue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface MedicalQueueRepository extends BaseRepository<MedicalQueue, UUID> {

    Optional<MedicalQueue> findByDoctorIdAndQueueDate(UUID doctorId, LocalDate queueDate);

    Optional<MedicalQueue> findByDoctorIdAndQueueDateForUpdate(UUID doctorId, LocalDate queueDate);

    Optional<MedicalQueue> findByIdForUpdate(UUID medicalQueueId);
}

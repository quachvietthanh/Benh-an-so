package com.benhsoan.port.outbound.repository.queue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.queue.MedicalQueue;
public interface MedicalQueueRepository {

    Optional<MedicalQueue> findById(UUID id);

    MedicalQueue save(MedicalQueue medicalQueue);

    Optional<MedicalQueue> findByDoctorIdAndQueueDate(UUID doctorId, LocalDate queueDate);

    Optional<MedicalQueue> findByDoctorIdAndQueueDateForUpdate(UUID doctorId, LocalDate queueDate);

    Optional<MedicalQueue> findByIdForUpdate(UUID medicalQueueId);

    boolean existsByDoctorIdAndQueueDateAndStatus(UUID doctorId, LocalDate queueDate,
            com.benhsoan.domain.queue.enums.MedicalQueueStatus status);
}

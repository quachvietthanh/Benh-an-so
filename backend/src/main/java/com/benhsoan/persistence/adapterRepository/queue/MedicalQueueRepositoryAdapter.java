package com.benhsoan.persistence.adapterRepository.queue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.persistence.entity.queue.MedicalQueueEntity;
import com.benhsoan.persistence.jpaRepository.queue.JpaMedicalQueueRepository;
import com.benhsoan.persistence.mapper.queue.MedicalQueuePersistenceMapper;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicalQueueRepositoryAdapter implements MedicalQueueRepository {

    private final JpaMedicalQueueRepository jpaRepository;
    private final MedicalQueuePersistenceMapper mapper;

    @Override
    public Optional<MedicalQueue> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public MedicalQueue save(MedicalQueue queue) {
        MedicalQueueEntity saved = jpaRepository.save(mapper.toEntity(queue));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<MedicalQueue> findByDoctorIdAndQueueDate(UUID doctorId, LocalDate queueDate) {
        return jpaRepository.findByDoctorIdAndQueueDate(doctorId, queueDate).map(mapper::toDomain);
    }

    @Override
    public Optional<MedicalQueue> findByDoctorIdAndQueueDateForUpdate(UUID doctorId, LocalDate queueDate) {
        return jpaRepository.findByDoctorIdAndQueueDateForUpdate(doctorId, queueDate).map(mapper::toDomain);
    }

    @Override
    public Optional<MedicalQueue> findByIdForUpdate(UUID medicalQueueId) {
        return jpaRepository.findByIdForUpdate(medicalQueueId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByDoctorIdAndQueueDateAndStatus(UUID doctorId, LocalDate queueDate,
            com.benhsoan.domain.queue.enums.MedicalQueueStatus status) {
        return jpaRepository.existsByDoctorIdAndQueueDateAndStatus(doctorId, queueDate, status);
    }
}

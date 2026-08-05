package com.benhsoan.persistence.adapterRepository.patient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.persistence.entity.patient.PatientChangeLogEntity;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientChangeLogRepository;
import com.benhsoan.persistence.mapper.patient.PatientChangeLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PatientChangeLogRepositoryAdapter
        implements PatientChangeLogRepository {

    private final JpaPatientChangeLogRepository jpaRepository;

    private final PatientChangeLogPersistenceMapper mapper;

    @Override
    public PatientChangeLog save(PatientChangeLog log) {

        PatientChangeLogEntity entity = mapper.toEntity(log);

        PatientChangeLogEntity saved = jpaRepository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PatientChangeLog> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Page<PatientChangeLog> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<PatientChangeLog> findByPatientId(
            UUID patientId,
            Pageable pageable
    ) {
        return jpaRepository.findByPatientId(patientId, pageable)
                .map(mapper::toDomain);
    }

}
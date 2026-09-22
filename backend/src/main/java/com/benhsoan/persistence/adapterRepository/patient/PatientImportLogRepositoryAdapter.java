package com.benhsoan.persistence.adapterRepository.patient;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.patient.PatientImportLog;
import com.benhsoan.persistence.entity.patient.PatientImportLogEntity;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientImportLogRepository;
import com.benhsoan.persistence.mapper.patient.PatientImportLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.patient.PatientImportLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PatientImportLogRepositoryAdapter implements PatientImportLogRepository {

    private final JpaPatientImportLogRepository jpaRepository;
    private final PatientImportLogPersistenceMapper mapper;

    @Override
    public PatientImportLog save(PatientImportLog log) {
        PatientImportLogEntity entity = mapper.toEntity(log);
        PatientImportLogEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PatientImportLog> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<PatientImportLog> findAll(Pageable pageable) {
        return jpaRepository.findAllByOrderByCreatedAtDesc(pageable).map(mapper::toDomain);
    }
}

package com.benhsoan.persistence.adapterRepository.medicalrecord;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;
import com.benhsoan.persistence.jpaRepository.medicalrecord.JpaMedicalRecordRepository;
import com.benhsoan.persistence.mapper.medicalrecord.MedicalRecordPersistenceMapper;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MedicalRecordRepositoryAdapter implements MedicalRecordRepository {

    private final JpaMedicalRecordRepository jpaRepository;
    private final MedicalRecordPersistenceMapper mapper;
    private final MedicalRecordCascadeDeleter cascadeDeleter;

    @Override
    public Optional<MedicalRecord> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<MedicalRecord> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public MedicalRecord save(MedicalRecord medicalRecord) {
        MedicalRecordEntity savedEntity = jpaRepository.save(mapper.toEntity(medicalRecord));
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MedicalRecord> findByVisitId(UUID visitId) {
        return jpaRepository.findByVisitId(visitId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByVisitId(UUID visitId) {
        return jpaRepository.existsByVisitId(visitId);
    }

    @Override
    public void deleteById(UUID id) {
        cascadeDeleter.deleteByMedicalRecordId(id);
        jpaRepository.deleteById(id);
    }
}

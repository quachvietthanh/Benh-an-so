package com.benhsoan.persistence.adapterRepository.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.patient.PatientFamilyHistory;
import com.benhsoan.persistence.entity.patient.PatientFamilyHistoryEntity;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientFamilyHistoryRepository;
import com.benhsoan.persistence.mapper.patient.PatientFamilyHistoryPersistenceMapper;
import com.benhsoan.port.outbound.repository.patient.PatientFamilyHistoryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PatientFamilyHistoryRepositoryAdapter implements PatientFamilyHistoryRepository {

    private final JpaPatientFamilyHistoryRepository jpaRepository;
    private final PatientFamilyHistoryPersistenceMapper mapper;

    @Override
    public PatientFamilyHistory save(PatientFamilyHistory familyHistory) {
        PatientFamilyHistoryEntity entity = mapper.toEntity(familyHistory);
        PatientFamilyHistoryEntity saved = jpaRepository.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PatientFamilyHistory> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PatientFamilyHistory> findByPatientIdAndActiveTrue(UUID patientId) {
        return jpaRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}

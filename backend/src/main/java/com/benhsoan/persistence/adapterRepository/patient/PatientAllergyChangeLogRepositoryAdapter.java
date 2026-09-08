package com.benhsoan.persistence.adapterRepository.patient;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.patient.PatientAllergyChangeLog;
import com.benhsoan.persistence.entity.patient.PatientAllergyChangeLogEntity;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientAllergyChangeLogRepository;
import com.benhsoan.persistence.mapper.patient.PatientAllergyChangeLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.patient.PatientAllergyChangeLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PatientAllergyChangeLogRepositoryAdapter implements PatientAllergyChangeLogRepository {

    private final JpaPatientAllergyChangeLogRepository jpaRepository;
    private final PatientAllergyChangeLogPersistenceMapper mapper;

    @Override
    public PatientAllergyChangeLog save(PatientAllergyChangeLog changeLog) {
        PatientAllergyChangeLogEntity entity = mapper.toEntity(changeLog);
        PatientAllergyChangeLogEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<PatientAllergyChangeLog> findByAllergyIdOrderByChangedAtDesc(UUID allergyId) {
        return jpaRepository.findByAllergyIdOrderByChangedAtDesc(allergyId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PatientAllergyChangeLog> findByPatientIdOrderByChangedAtDesc(UUID patientId) {
        return jpaRepository.findByPatientIdOrderByChangedAtDesc(patientId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}

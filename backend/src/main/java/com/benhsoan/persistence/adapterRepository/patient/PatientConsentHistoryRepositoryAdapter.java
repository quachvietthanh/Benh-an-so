package com.benhsoan.persistence.adapterRepository.patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.patient.PatientConsentRecord;
import com.benhsoan.persistence.entity.patient.PatientConsentHistoryEntity;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientConsentHistoryRepository;
import com.benhsoan.persistence.mapper.patient.PatientConsentHistoryPersistenceMapper;
import com.benhsoan.port.outbound.repository.patient.PatientConsentHistoryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PatientConsentHistoryRepositoryAdapter
        implements PatientConsentHistoryRepository {

    private final JpaPatientConsentHistoryRepository jpaRepository;
    private final PatientConsentHistoryPersistenceMapper mapper;

    @Override
    public PatientConsentRecord save(PatientConsentRecord record) {
        PatientConsentHistoryEntity entity = mapper.toEntity(record);
        PatientConsentHistoryEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<PatientConsentRecord> findByPatientId(UUID patientId) {
        return jpaRepository.findByPatientIdOrderByVersionNumberDesc(patientId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PatientConsentRecord> findLatestByPatientId(UUID patientId) {
        return jpaRepository.findTopByPatientIdOrderByVersionNumberDesc(patientId)
                .map(mapper::toDomain);
    }

    @Override
    public int getNextVersionNumber(UUID patientId) {
        return jpaRepository.findTopByPatientIdOrderByVersionNumberDesc(patientId)
                .map(e -> e.getVersionNumber() + 1)
                .orElse(1);
    }
}

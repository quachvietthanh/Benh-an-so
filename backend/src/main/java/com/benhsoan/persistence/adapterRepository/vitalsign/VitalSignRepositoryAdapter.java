package com.benhsoan.persistence.adapterRepository.vitalsign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.vitalsign.VitalSign;
import com.benhsoan.persistence.entity.vitalsign.VitalSignEntity;
import com.benhsoan.persistence.jpaRepository.vitalsign.JpaVitalSignRepository;
import com.benhsoan.persistence.mapper.vitalsign.VitalSignPersistenceMapper;
import com.benhsoan.port.outbound.repository.vitalsign.VitalSignRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VitalSignRepositoryAdapter implements VitalSignRepository {

    private final JpaVitalSignRepository jpaRepository;
    private final VitalSignPersistenceMapper mapper;

    @Override
    public VitalSign save(VitalSign vitalSign) {
        VitalSignEntity entity = mapper.toEntity(vitalSign);
        VitalSignEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<VitalSign> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<VitalSign> findLatestByVisitId(UUID visitId) {
        return jpaRepository.findFirstByVisitIdOrderByRecordedAtDesc(visitId).map(mapper::toDomain);
    }

    @Override
    public List<VitalSign> findByVisitId(UUID visitId) {
        return jpaRepository.findByVisitIdOrderByRecordedAtDesc(visitId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<VitalSign> findHistoryByPatientId(UUID patientId) {
        return jpaRepository.findByPatientIdOrderByRecordedAtAsc(patientId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByVisitId(UUID visitId) {
        return jpaRepository.existsByVisitId(visitId);
    }
}

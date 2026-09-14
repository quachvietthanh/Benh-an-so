package com.benhsoan.persistence.adapterRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.clinical.ClinicalReferenceRange;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalReferenceRangeRepository;
import com.benhsoan.persistence.mapper.clinical.ClinicalReferenceRangePersistenceMapper;
import com.benhsoan.port.outbound.repository.clinical.ClinicalReferenceRangeRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClinicalReferenceRangeRepositoryAdapter implements ClinicalReferenceRangeRepository {

    private final JpaClinicalReferenceRangeRepository jpaRepository;
    private final ClinicalReferenceRangePersistenceMapper mapper;

    @Override
    public ClinicalReferenceRange save(ClinicalReferenceRange range) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(range)));
    }

    @Override
    public Optional<ClinicalReferenceRange> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ClinicalReferenceRange> findByClinicalServiceId(UUID clinicalServiceId) {
        return jpaRepository.findByClinicalServiceIdOrderByCreatedAtAscIdAsc(clinicalServiceId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<ClinicalReferenceRange> findByClinicalServiceIdIn(Collection<UUID> clinicalServiceIds) {
        if (clinicalServiceIds == null || clinicalServiceIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByClinicalServiceIdInOrderByCreatedAtAscIdAsc(clinicalServiceIds).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<ClinicalReferenceRange> findActiveByClinicalServiceId(UUID clinicalServiceId) {
        return jpaRepository.findByClinicalServiceIdAndActiveTrueOrderByCreatedAtAscIdAsc(clinicalServiceId).stream()
                .map(mapper::toDomain).toList();
    }
}

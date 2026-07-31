package com.benhsoan.persistence.adapterRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalResultRepository;
import com.benhsoan.persistence.mapper.clinical.ClinicalResultPersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalResultRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClinicalResultRepositoryAdapter implements ClinicalResultRepository {
    private final JpaClinicalResultRepository jpaRepository;
    private final ClinicalResultPersistenceMapper mapper;
    public Optional<ClinicalResult> findById(UUID id) { return jpaRepository.findById(id).map(mapper::toDomain); }
    public ClinicalResult save(ClinicalResult result) { return mapper.toDomain(jpaRepository.save(mapper.toEntity(result))); }
    public void deleteById(UUID id) { if (id != null) jpaRepository.deleteById(id); }
    public Optional<ClinicalResult> findByClinicalOrderItemId(UUID itemId) { return jpaRepository.findByClinicalOrderItemId(itemId).map(mapper::toDomain); }
    public Page<ClinicalResult> findByVisitId(UUID visitId, Pageable pageable) { return jpaRepository.findByVisitIdOrderByEnteredAtDesc(visitId, pageable).map(mapper::toDomain); }
    public List<ClinicalResult> findByClinicalOrderItemIdIn(Collection<UUID> itemIds) {
        return itemIds == null || itemIds.isEmpty() ? List.of() : jpaRepository.findByClinicalOrderItemIdIn(itemIds).stream().map(mapper::toDomain).toList();
    }
}

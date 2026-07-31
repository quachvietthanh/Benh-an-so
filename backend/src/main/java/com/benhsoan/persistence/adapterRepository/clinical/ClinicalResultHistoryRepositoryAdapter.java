package com.benhsoan.persistence.adapterRepository.clinical;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.clinical.ClinicalResultHistory;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalResultHistoryRepository;
import com.benhsoan.persistence.mapper.clinical.ClinicalResultHistoryPersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalResultHistoryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClinicalResultHistoryRepositoryAdapter implements ClinicalResultHistoryRepository {
    private final JpaClinicalResultHistoryRepository jpaRepository;
    private final ClinicalResultHistoryPersistenceMapper mapper;
    public ClinicalResultHistory save(ClinicalResultHistory history) { return mapper.toDomain(jpaRepository.save(mapper.toEntity(history))); }
    public List<ClinicalResultHistory> findByClinicalResultId(UUID resultId) { return jpaRepository.findByClinicalResultIdOrderByChangedAtDesc(resultId).stream().map(mapper::toDomain).toList(); }
}

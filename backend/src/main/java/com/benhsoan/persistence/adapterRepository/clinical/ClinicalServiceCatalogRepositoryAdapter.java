package com.benhsoan.persistence.adapterRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.persistence.entity.clinical.ClinicalServiceCatalogEntity;
import com.benhsoan.persistence.jpaRepository.clinical.JpaClinicalServiceCatalogRepository;
import com.benhsoan.persistence.mapper.clinical.ClinicalServiceCatalogPersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalServiceCatalogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClinicalServiceCatalogRepositoryAdapter implements ClinicalServiceCatalogRepository {

    private final JpaClinicalServiceCatalogRepository jpaRepository;
    private final ClinicalServiceCatalogPersistenceMapper mapper;

    @Override
    public Optional<ClinicalServiceCatalog> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public ClinicalServiceCatalog save(ClinicalServiceCatalog service) {
        ClinicalServiceCatalogEntity savedEntity = jpaRepository.save(mapper.toEntity(service));
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void deleteById(UUID id) {
        if (id != null) {
            jpaRepository.deleteById(id);
        }
    }

    @Override
    public Page<ClinicalServiceCatalog> findActiveByKeyword(String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return jpaRepository.findActiveByKeyword(normalizedKeyword, pageable).map(mapper::toDomain);
    }

    @Override
    public List<ClinicalServiceCatalog> findActiveByIdIn(Collection<UUID> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByIdInAndActiveTrue(serviceIds).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

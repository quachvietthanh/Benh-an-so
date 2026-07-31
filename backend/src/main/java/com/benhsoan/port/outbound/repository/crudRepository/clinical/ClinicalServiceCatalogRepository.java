package com.benhsoan.port.outbound.repository.crudRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface ClinicalServiceCatalogRepository extends BaseRepository<ClinicalServiceCatalog, UUID> {

    Page<ClinicalServiceCatalog> findActiveByKeyword(String keyword, Pageable pageable);

    List<ClinicalServiceCatalog> findActiveByIdIn(Collection<UUID> serviceIds);
}

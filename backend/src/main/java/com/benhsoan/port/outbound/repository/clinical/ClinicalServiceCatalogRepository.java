package com.benhsoan.port.outbound.repository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
public interface ClinicalServiceCatalogRepository {

    Page<ClinicalServiceCatalog> findActiveByKeyword(String keyword, Pageable pageable);

    List<ClinicalServiceCatalog> findActiveByIdIn(Collection<UUID> serviceIds);
}

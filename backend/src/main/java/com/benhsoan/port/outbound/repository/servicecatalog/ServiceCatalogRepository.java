package com.benhsoan.port.outbound.repository.servicecatalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.servicecatalog.ServiceCatalog;

public interface ServiceCatalogRepository {

    ServiceCatalog save(ServiceCatalog serviceCatalog);

    Optional<ServiceCatalog> findById(UUID id);

    Optional<ServiceCatalog> findByServiceCode(String serviceCode);

    List<ServiceCatalog> findAll();

    Page<ServiceCatalog> search(String keyword, Boolean active, Pageable pageable);

    boolean existsByServiceCode(String serviceCode);

    boolean existsByNormalizedServiceName(String normalizedServiceName, UUID excludedId);
}

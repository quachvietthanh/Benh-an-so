package com.benhsoan.port.outbound.repository.servicecatalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.servicecatalog.ServiceCatalog;

public interface ServiceCatalogRepository {

    ServiceCatalog save(ServiceCatalog serviceCatalog);

    Optional<ServiceCatalog> findById(UUID id);

    Optional<ServiceCatalog> findByServiceCode(String serviceCode);

    List<ServiceCatalog> findAll();

    boolean existsByServiceCode(String serviceCode);

    boolean existsByNormalizedServiceName(String normalizedServiceName, UUID excludedId);
}

package com.benhsoan.port.outbound.repository.servicecatalog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.servicecatalog.ServicePrice;

public interface ServicePriceRepository {

    ServicePrice save(ServicePrice servicePrice);

    Optional<ServicePrice> findById(UUID id);

    List<ServicePrice> findAllByServiceCatalogId(UUID serviceCatalogId);

    Optional<ServicePrice> findEffectivePrice(UUID serviceCatalogId, LocalDate effectiveOn);

    boolean existsByServiceCatalogIdAndEffectiveFrom(UUID serviceCatalogId, LocalDate effectiveFrom);
}

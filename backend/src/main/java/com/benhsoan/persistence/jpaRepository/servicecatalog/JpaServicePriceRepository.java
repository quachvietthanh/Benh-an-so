package com.benhsoan.persistence.jpaRepository.servicecatalog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.persistence.entity.servicecatalog.ServicePriceEntity;

public interface JpaServicePriceRepository extends JpaRepository<ServicePriceEntity, UUID> {

    List<ServicePriceEntity> findAllByServiceCatalogIdOrderByEffectiveFromDesc(UUID serviceCatalogId);

    Optional<ServicePriceEntity>
            findFirstByServiceCatalogIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                    UUID serviceCatalogId,
                    LocalDate effectiveOn
            );

    boolean existsByServiceCatalogIdAndEffectiveFrom(UUID serviceCatalogId, LocalDate effectiveFrom);
}

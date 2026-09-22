package com.benhsoan.persistence.jpaRepository.servicecatalog;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.servicecatalog.ServicePriceEntity;

public interface JpaServicePriceRepository extends JpaRepository<ServicePriceEntity, UUID> {

    List<ServicePriceEntity> findAllByServiceCatalogIdOrderByEffectiveFromDesc(UUID serviceCatalogId);

    Optional<ServicePriceEntity>
            findFirstByServiceCatalogIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                    UUID serviceCatalogId,
                    LocalDate effectiveOn
            );

    @Query("""
            select sp
            from ServicePriceEntity sp
            where sp.serviceCatalogId in :serviceCatalogIds
              and sp.effectiveFrom <= :effectiveOn
              and sp.effectiveFrom = (
                  select max(sp2.effectiveFrom)
                  from ServicePriceEntity sp2
                  where sp2.serviceCatalogId = sp.serviceCatalogId
                    and sp2.effectiveFrom <= :effectiveOn
              )
            """)
    List<ServicePriceEntity> findEffectivePrices(
            @Param("serviceCatalogIds") Collection<UUID> serviceCatalogIds,
            @Param("effectiveOn") LocalDate effectiveOn
    );

    boolean existsByServiceCatalogIdAndEffectiveFrom(UUID serviceCatalogId, LocalDate effectiveFrom);
}


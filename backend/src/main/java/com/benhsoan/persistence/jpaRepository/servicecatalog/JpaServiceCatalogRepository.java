package com.benhsoan.persistence.jpaRepository.servicecatalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.servicecatalog.ServiceCatalogEntity;

public interface JpaServiceCatalogRepository extends JpaRepository<ServiceCatalogEntity, UUID> {

    Optional<ServiceCatalogEntity> findByServiceCodeIgnoreCase(String serviceCode);

    boolean existsByServiceCodeIgnoreCase(String serviceCode);

    List<ServiceCatalogEntity> findAllByOrderByServiceNameAscServiceCodeAsc();

    @Query("""
            select count(service) > 0 from ServiceCatalogEntity service
            where lower(trim(service.serviceName)) = :normalizedServiceName
              and (:excludedId is null or service.id <> :excludedId)
            """)
    boolean existsByNormalizedServiceName(
            @Param("normalizedServiceName") String normalizedServiceName,
            @Param("excludedId") UUID excludedId
    );
}

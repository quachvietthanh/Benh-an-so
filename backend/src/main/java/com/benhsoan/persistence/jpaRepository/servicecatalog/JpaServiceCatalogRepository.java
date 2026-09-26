package com.benhsoan.persistence.jpaRepository.servicecatalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.servicecatalog.ServiceCatalogEntity;

public interface JpaServiceCatalogRepository extends JpaRepository<ServiceCatalogEntity, UUID> {

    Optional<ServiceCatalogEntity> findByServiceCodeIgnoreCase(String serviceCode);

    boolean existsByServiceCodeIgnoreCase(String serviceCode);

    List<ServiceCatalogEntity> findAllByOrderByServiceNameAscServiceCodeAsc();

    @Query("""
            select service from ServiceCatalogEntity service
            where (:keyword = ''
                   or lower(service.serviceCode) like lower(concat('%', :keyword, '%'))
                   or lower(service.serviceName) like lower(concat('%', :keyword, '%')))
              and (:active is null or service.active = :active)
            """)
    Page<ServiceCatalogEntity> search(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable
    );

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

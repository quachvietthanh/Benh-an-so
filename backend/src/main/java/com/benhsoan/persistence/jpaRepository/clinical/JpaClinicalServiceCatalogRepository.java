package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.benhsoan.persistence.entity.clinical.ClinicalServiceCatalogEntity;

public interface JpaClinicalServiceCatalogRepository extends JpaRepository<ClinicalServiceCatalogEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select service from ClinicalServiceCatalogEntity service where service.id = :id")
    Optional<ClinicalServiceCatalogEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select service from ClinicalServiceCatalogEntity service
            where service.active = true
              and (:keyword = ''
                   or lower(service.serviceCode) like lower(concat('%', :keyword, '%'))
                   or lower(service.serviceName) like lower(concat('%', :keyword, '%')))
            """)
    Page<ClinicalServiceCatalogEntity> findActiveByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<ClinicalServiceCatalogEntity> findByIdInAndActiveTrue(Collection<UUID> serviceIds);

    boolean existsByServiceCode(String serviceCode);

    @Query("""
            SELECT service
            FROM ClinicalServiceCatalogEntity service
            WHERE (:keyword = ''
                    OR lower(service.serviceCode) LIKE lower(concat('%', :keyword, '%'))
                    OR lower(service.serviceName) LIKE lower(concat('%', :keyword, '%')))
              AND (:active IS NULL OR service.active = :active)
            ORDER BY service.serviceName ASC, service.serviceCode ASC
            """)
    Page<ClinicalServiceCatalogEntity> search(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable
    );
}

package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.persistence.entity.clinical.ClinicalServiceCatalogEntity;

public interface JpaClinicalServiceCatalogRepository extends JpaRepository<ClinicalServiceCatalogEntity, UUID> {

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
}

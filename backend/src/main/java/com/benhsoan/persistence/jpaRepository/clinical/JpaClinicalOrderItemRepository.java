package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderItemEntity;

public interface JpaClinicalOrderItemRepository extends JpaRepository<ClinicalOrderItemEntity, UUID> {

    List<ClinicalOrderItemEntity> findByClinicalOrderIdIn(Collection<UUID> clinicalOrderIds);

    @Query("""
            select item.id as clinicalOrderItemId,
                   service.serviceCatalogId as serviceCatalogId,
                   item.serviceName as serviceName
            from ClinicalOrderItemEntity item
            join ClinicalOrderEntity clinicalOrder on clinicalOrder.id = item.clinicalOrderId
            join ClinicalServiceCatalogEntity service on service.id = item.clinicalServiceId
            where clinicalOrder.visitId = :visitId
              and item.status = :status
            order by item.createdAt, item.id
            """)
    List<BillableClinicalServiceView> findBillableByVisitId(
            @Param("visitId") UUID visitId,
            @Param("status") ClinicalOrderItemStatus status
    );

    boolean existsByClinicalOrderIdAndClinicalServiceId(UUID clinicalOrderId, UUID clinicalServiceId);
}

package com.benhsoan.persistence.jpaRepository.clinical;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderItemEntity;

import jakarta.persistence.LockModeType;

public interface JpaClinicalOrderItemRepository extends JpaRepository<ClinicalOrderItemEntity, UUID> {

    List<ClinicalOrderItemEntity> findByClinicalOrderIdIn(Collection<UUID> clinicalOrderIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from ClinicalOrderItemEntity item where item.id = :id")
    Optional<ClinicalOrderItemEntity> findByIdForUpdate(@Param("id") UUID id);

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

    @Query("select item.id from ClinicalOrderItemEntity item where item.clinicalOrderId in :orderIds")
    List<UUID> findIdsByClinicalOrderIdIn(@Param("orderIds") Collection<UUID> orderIds);

    @Modifying
    @Query("delete from ClinicalOrderItemEntity item where item.clinicalOrderId in :orderIds")
    void deleteByClinicalOrderIdIn(@Param("orderIds") Collection<UUID> orderIds);

    @Query("""
            select item.id as orderItemId,
                   clinicalOrder.id as orderId,
                   clinicalOrder.orderCode as orderCode,
                   visit.id as visitId,
                   visit.visitCode as visitCode,
                   patient.id as patientId,
                   patient.patientCode as patientCode,
                   patient.fullName as patientFullName,
                   doctor.id as doctorId,
                   doctor.fullName as doctorFullName,
                   item.clinicalServiceId as clinicalServiceId,
                   item.serviceCode as serviceCode,
                   item.serviceName as serviceName,
                   service.serviceType as serviceType,
                   item.instruction as instruction,
                   clinicalOrder.clinicalReason as clinicalReason,
                   item.status as status,
                   clinicalOrder.orderedAt as orderedAt
            from ClinicalOrderItemEntity item
            join ClinicalOrderEntity clinicalOrder on clinicalOrder.id = item.clinicalOrderId
            join ClinicalServiceCatalogEntity service on service.id = item.clinicalServiceId
            join VisitEntity visit on visit.id = clinicalOrder.visitId
            join PatientEntity patient on patient.id = clinicalOrder.patientId
            join UserEntity doctor on doctor.id = clinicalOrder.orderedBy
            where item.status = :status
              and visit.status in (com.benhsoan.domain.visit.enums.VisitStatus.WAITING, com.benhsoan.domain.visit.enums.VisitStatus.IN_PROGRESS, com.benhsoan.domain.visit.enums.VisitStatus.WAITING_FOR_RESULT)
              and clinicalOrder.status not in (com.benhsoan.domain.clinical.enums.ClinicalOrderStatus.CANCELLED, com.benhsoan.domain.clinical.enums.ClinicalOrderStatus.COMPLETED)
              and (:patientId is null or clinicalOrder.patientId = :patientId)
              and (:doctorId is null or clinicalOrder.orderedBy = :doctorId)
              and (:fromDate is null or clinicalOrder.orderedAt >= :fromDate)
              and (:toDate is null or clinicalOrder.orderedAt <= :toDate)
            order by clinicalOrder.orderedAt asc, item.id asc
            """)
    Page<PendingClinicalOrderItemView> findPendingItems(
            @Param("status") ClinicalOrderItemStatus status,
            @Param("patientId") UUID patientId,
            @Param("doctorId") UUID doctorId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );

    @Query("""
            select count(item.id)
            from ClinicalOrderItemEntity item
            join ClinicalOrderEntity clinicalOrder on clinicalOrder.id = item.clinicalOrderId
            where clinicalOrder.visitId = :visitId
              and item.status = :status
            """)
    long countByVisitIdAndStatus(
            @Param("visitId") UUID visitId,
            @Param("status") ClinicalOrderItemStatus status
    );

    @Query("""
            select distinct item.serviceName
            from ClinicalOrderItemEntity item
            join ClinicalOrderEntity clinicalOrder on clinicalOrder.id = item.clinicalOrderId
            where clinicalOrder.visitId = :visitId
              and item.status = :status
            order by item.serviceName asc
            """)
    List<String> findPendingServiceNamesByVisitId(
            @Param("visitId") UUID visitId,
            @Param("status") ClinicalOrderItemStatus status
    );
}

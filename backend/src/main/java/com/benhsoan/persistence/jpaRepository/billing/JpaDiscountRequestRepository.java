package com.benhsoan.persistence.jpaRepository.billing;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.billing.enums.DiscountRequestStatus;
import com.benhsoan.domain.billing.enums.DiscountType;
import com.benhsoan.persistence.entity.billing.DiscountRequestEntity;

public interface JpaDiscountRequestRepository
        extends JpaRepository<DiscountRequestEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select dr from DiscountRequestEntity dr where dr.id = :id")
    Optional<DiscountRequestEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<DiscountRequestEntity> findByVisitIdAndStatus(
            UUID visitId,
            DiscountRequestStatus status
    );

    Optional<DiscountRequestEntity> findFirstByVisitIdAndStatusOrderByApprovedAtDesc(
            UUID visitId,
            DiscountRequestStatus status
    );

    List<DiscountRequestEntity> findByVisitIdOrderByRequestedAtDesc(UUID visitId);

    boolean existsByVisitIdAndStatus(UUID visitId, DiscountRequestStatus status);

    @Query(
            value = """
                    select dr
                    from DiscountRequestEntity dr
                    where (:visitId is null or dr.visitId = :visitId)
                      and (:status is null or dr.status = :status)
                      and (:discountType is null or dr.discountType = :discountType)
                      and (:requestedBy is null or dr.requestedBy = :requestedBy)
                      and (:approvedBy is null or dr.approvedBy = :approvedBy)
                      and (:requestedFrom is null or dr.requestedAt >= :requestedFrom)
                      and (:requestedTo is null or dr.requestedAt <= :requestedTo)
                    order by dr.requestedAt desc
                    """,
            countQuery = """
                    select count(dr)
                    from DiscountRequestEntity dr
                    where (:visitId is null or dr.visitId = :visitId)
                      and (:status is null or dr.status = :status)
                      and (:discountType is null or dr.discountType = :discountType)
                      and (:requestedBy is null or dr.requestedBy = :requestedBy)
                      and (:approvedBy is null or dr.approvedBy = :approvedBy)
                      and (:requestedFrom is null or dr.requestedAt >= :requestedFrom)
                      and (:requestedTo is null or dr.requestedAt <= :requestedTo)
                    """
    )
    Page<DiscountRequestEntity> search(
            @Param("visitId") UUID visitId,
            @Param("status") DiscountRequestStatus status,
            @Param("discountType") DiscountType discountType,
            @Param("requestedBy") UUID requestedBy,
            @Param("approvedBy") UUID approvedBy,
            @Param("requestedFrom") Instant requestedFrom,
            @Param("requestedTo") Instant requestedTo,
            Pageable pageable
    );
}

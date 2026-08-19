package com.benhsoan.persistence.jpaRepository.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.persistence.entity.billing.PaymentEntity;

import jakarta.persistence.LockModeType;

public interface JpaPaymentRepository
        extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByVisitId(UUID visitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from PaymentEntity payment where payment.id = :paymentId")
    Optional<PaymentEntity> findByIdForUpdate(@Param("paymentId") UUID paymentId);

    @Query("""
            select sum(payment.amountPaid)
            from PaymentEntity payment
            where payment.paidAt >= :fromInclusive
              and payment.paidAt < :toExclusive
              and payment.status in :statuses
            """)
    BigDecimal sumAmountPaidBetween(
            @Param("statuses") Collection<PaymentStatus> statuses,
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
            select sum(payment.amountPaid)
            from PaymentEntity payment
            where payment.status = com.benhsoan.domain.billing.enums.PaymentStatus.REFUNDED
              and payment.refundedAt >= :fromInclusive
              and payment.refundedAt < :toExclusive
            """)
    BigDecimal sumRefundedAmountBetween(
            @Param("fromInclusive") Instant fromInclusive,
            @Param("toExclusive") Instant toExclusive
    );
}

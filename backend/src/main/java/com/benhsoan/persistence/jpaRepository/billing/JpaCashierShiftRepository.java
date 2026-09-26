package com.benhsoan.persistence.jpaRepository.billing;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.benhsoan.domain.billing.enums.CashierShiftStatus;
import com.benhsoan.persistence.entity.billing.CashierShiftEntity;

public interface JpaCashierShiftRepository extends JpaRepository<CashierShiftEntity, UUID> {

    Optional<CashierShiftEntity> findByShiftCode(String shiftCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CashierShiftEntity s WHERE s.id = :id")
    Optional<CashierShiftEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT s FROM CashierShiftEntity s
            WHERE (:cashierId IS NULL OR s.cashierId = :cashierId)
              AND (:status IS NULL OR s.status = :status)
              AND (:from IS NULL OR s.createdAt >= :from)
              AND (:to IS NULL OR s.createdAt <= :to)
            """)
    Page<CashierShiftEntity> search(
            @Param("cashierId") UUID cashierId,
            @Param("status") CashierShiftStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
